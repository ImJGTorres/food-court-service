package com.pragma.powerup.infrastructure.out.jpa.adapter;

import com.pragma.powerup.domain.model.OrderModel;
import com.pragma.powerup.domain.model.OrderStatus;
import com.pragma.powerup.domain.model.PageModel;
import com.pragma.powerup.infrastructure.out.jpa.entity.OrderEntity;
import com.pragma.powerup.infrastructure.out.jpa.mapper.IOrderEntityMapper;
import com.pragma.powerup.infrastructure.out.jpa.repository.IOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderJpaAdapterTest {
    private static final Long CLIENT_ID = 20L;
    private static final Long RESTAURANT_ID = 1L;
    private static final Long ORDER_ID = 7L;
    private static final List<OrderStatus> IN_PROGRESS_STATUSES =
            List.of(OrderStatus.PENDING, OrderStatus.IN_PREPARATION, OrderStatus.READY);

    @Mock
    private IOrderRepository orderRepository;

    @Mock
    private IOrderEntityMapper orderEntityMapper;

    @InjectMocks
    private OrderJpaAdapter orderJpaAdapter;

    @Test
    void saveOrderConvertsTheModelFlushesTheEntityAtOnceAndReturnsTheSavedOrderAsModel() {
        OrderModel orderToSave = new OrderModel();
        OrderEntity entityToSave = new OrderEntity();
        OrderEntity savedEntity = new OrderEntity();
        OrderModel savedOrder = new OrderModel();
        when(orderEntityMapper.toEntity(orderToSave)).thenReturn(entityToSave);
        when(orderRepository.saveAndFlush(entityToSave)).thenReturn(savedEntity);
        when(orderEntityMapper.toModel(savedEntity)).thenReturn(savedOrder);

        OrderModel result = orderJpaAdapter.saveOrder(orderToSave);

        assertSame(savedOrder, result);
        verify(orderRepository).saveAndFlush(entityToSave);
    }

    @Test
    void existsByClientIdAndStatusInReturnsTrueWhenTheRepositoryFindsAnOrder() {
        when(orderRepository.existsByClientIdAndStatusIn(CLIENT_ID, IN_PROGRESS_STATUSES)).thenReturn(true);

        assertTrue(orderJpaAdapter.existsByClientIdAndStatusIn(CLIENT_ID, IN_PROGRESS_STATUSES));
    }

    @Test
    void existsByClientIdAndStatusInReturnsFalseWhenTheRepositoryFindsNothing() {
        when(orderRepository.existsByClientIdAndStatusIn(CLIENT_ID, IN_PROGRESS_STATUSES)).thenReturn(false);

        assertFalse(orderJpaAdapter.existsByClientIdAndStatusIn(CLIENT_ID, IN_PROGRESS_STATUSES));
    }

    @Test
    void findOrdersByRestaurantAndStatusAsksForThePageSortedFromTheOldestOrder() {
        when(orderRepository.findByRestaurantIdAndStatus(eq(RESTAURANT_ID), eq(OrderStatus.PENDING), any(Pageable.class)))
                .thenReturn(Page.empty());

        orderJpaAdapter.findOrdersByRestaurantAndStatus(RESTAURANT_ID, OrderStatus.PENDING, 2, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findByRestaurantIdAndStatus(eq(RESTAURANT_ID), eq(OrderStatus.PENDING),
                pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertAll(
                () -> assertEquals(2, pageable.getPageNumber()),
                () -> assertEquals(5, pageable.getPageSize()),
                () -> assertEquals(Sort.by("date").ascending(), pageable.getSort())
        );
    }

    @Test
    void findOrdersByRestaurantAndStatusConvertsTheSpringPageIntoThePageModel() {
        OrderEntity entity = new OrderEntity();
        OrderModel model = new OrderModel();
        Page<OrderEntity> springPage = new PageImpl<>(List.of(entity), PageRequest.of(0, 1), 3);
        when(orderRepository.findByRestaurantIdAndStatus(eq(RESTAURANT_ID), eq(OrderStatus.READY), any(Pageable.class)))
                .thenReturn(springPage);
        when(orderEntityMapper.toModel(entity)).thenReturn(model);

        PageModel<OrderModel> result = orderJpaAdapter.findOrdersByRestaurantAndStatus(
                RESTAURANT_ID, OrderStatus.READY, 0, 1);

        assertAll(
                () -> assertSame(model, result.getContent().get(0)),
                () -> assertEquals(3L, result.getTotalElements()),
                () -> assertEquals(3, result.getTotalPages())
        );
    }

    @Test
    void findOrderByIdReturnsTheOrderAsModelWhenItExists() {
        OrderEntity entity = new OrderEntity();
        OrderModel model = new OrderModel();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(entity));
        when(orderEntityMapper.toModel(entity)).thenReturn(model);

        Optional<OrderModel> result = orderJpaAdapter.findOrderById(ORDER_ID);

        assertTrue(result.isPresent());
        assertSame(model, result.get());
    }

    @Test
    void findOrderByIdReturnsEmptyWhenTheOrderDoesNotExist() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertTrue(orderJpaAdapter.findOrderById(ORDER_ID).isEmpty());
    }
}
