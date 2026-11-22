package com.pragma.powerup.infrastructure.out.jpa.adapter;

import com.pragma.powerup.domain.model.OrderModel;
import com.pragma.powerup.domain.model.OrderStatus;
import com.pragma.powerup.infrastructure.out.jpa.entity.OrderEntity;
import com.pragma.powerup.infrastructure.out.jpa.mapper.IOrderEntityMapper;
import com.pragma.powerup.infrastructure.out.jpa.repository.IOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderJpaAdapterTest {
    private static final Long CLIENT_ID = 20L;
    private static final List<OrderStatus> IN_PROGRESS_STATUSES =
            List.of(OrderStatus.PENDING, OrderStatus.IN_PREPARATION, OrderStatus.READY);

    @Mock
    private IOrderRepository orderRepository;

    @Mock
    private IOrderEntityMapper orderEntityMapper;

    @InjectMocks
    private OrderJpaAdapter orderJpaAdapter;

    @Test
    void saveOrderConvertsTheModelSavesTheEntityAndReturnsTheSavedOrderAsModel() {
        OrderModel orderToSave = new OrderModel();
        OrderEntity entityToSave = new OrderEntity();
        OrderEntity savedEntity = new OrderEntity();
        OrderModel savedOrder = new OrderModel();
        when(orderEntityMapper.toEntity(orderToSave)).thenReturn(entityToSave);
        when(orderRepository.save(entityToSave)).thenReturn(savedEntity);
        when(orderEntityMapper.toModel(savedEntity)).thenReturn(savedOrder);

        OrderModel result = orderJpaAdapter.saveOrder(orderToSave);

        assertSame(savedOrder, result);
        verify(orderRepository).save(entityToSave);
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
}
