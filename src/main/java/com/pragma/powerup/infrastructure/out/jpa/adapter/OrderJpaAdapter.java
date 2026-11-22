package com.pragma.powerup.infrastructure.out.jpa.adapter;

import com.pragma.powerup.domain.model.OrderModel;
import com.pragma.powerup.domain.model.OrderStatus;
import com.pragma.powerup.domain.model.PageModel;
import com.pragma.powerup.domain.spi.IOrderPersistencePort;
import com.pragma.powerup.infrastructure.out.jpa.entity.OrderEntity;
import com.pragma.powerup.infrastructure.out.jpa.mapper.IOrderEntityMapper;
import com.pragma.powerup.infrastructure.out.jpa.repository.IOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@RequiredArgsConstructor
public class OrderJpaAdapter implements IOrderPersistencePort {
    private static final String DATE_FIELD = "date";

    private final IOrderRepository orderRepository;
    private final IOrderEntityMapper orderEntityMapper;

    @Override
    public OrderModel saveOrder(OrderModel orderModel) {
        OrderEntity savedOrder = orderRepository.save(orderEntityMapper.toEntity(orderModel));
        return orderEntityMapper.toModel(savedOrder);
    }

    @Override
    public boolean existsByClientIdAndStatusIn(Long clientId, List<OrderStatus> statuses) {
        return orderRepository.existsByClientIdAndStatusIn(clientId, statuses);
    }

    @Override
    public PageModel<OrderModel> findOrdersByRestaurantAndStatus(Long restaurantId, OrderStatus status, int page,
                                                                 int size) {
        return PageModelConverter.toPageModel(
                orderRepository.findByRestaurantIdAndStatus(restaurantId, status,
                        PageRequest.of(page, size, Sort.by(DATE_FIELD).ascending())),
                orderEntityMapper::toModel);
    }
}
