package hongik.Todoing.domain.order.adaptor;

import hongik.Todoing.Common.annotation.Adaptor;
import hongik.Todoing.domain.order.repository.OrderRepository;
import hongik.Todoing.domain.order.domain.order.Order;
import hongik.Todoing.domain.order.exception.orderException.OrderNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Adaptor
@RequiredArgsConstructor
public class OrderAdaptor {

    private final OrderRepository orderRepository;

    public Order save(Order order) {
        return orderRepository.save(order);
    }

    public void saveWithTid(Order order, String tid) {
        order.updateTidAfterReady(tid);
        orderRepository.save(order);
    }

    public Order findById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> OrderNotFoundException.EXCEPTION);
    }

    public List<Order> findByUserId(Long userId) {
        return orderRepository.findAllByUserId(userId);
    }


    public Order findByTid(String tid) {
        return orderRepository.findByTid(tid)
                .orElseThrow(() -> OrderNotFoundException.EXCEPTION);
    }

    public Order findByPartnerOrderId(String partnerOrderId) {
        return orderRepository.findByPartnerOrderId(partnerOrderId);
    }
}
