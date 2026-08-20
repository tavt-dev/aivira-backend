package com.tien.aivirabackend.service.commerce;

import java.time.Instant;

import jakarta.persistence.criteria.JoinType;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.domain.entity.transaction.Order;

@Component
public class OrderSpecifications {
    public Specification<Order> adminOrders(OrderStatus status, String keyword, Instant fromDate, Instant toDate) {
        return Specification.where(hasStatus(status)).and(createdFrom(fromDate)).and(createdTo(toDate))
                .and(keywordContains(keyword));
    }

    private Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, criteriaBuilder) -> status == null ? null
                : criteriaBuilder.equal(root.get("orderStatus"), status);
    }

    private Specification<Order> createdFrom(Instant fromDate) {
        return (root, query, criteriaBuilder) -> fromDate == null ? null
                : criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate);
    }

    private Specification<Order> createdTo(Instant toDate) {
        return (root, query, criteriaBuilder) -> toDate == null ? null
                : criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toDate);
    }

    private Specification<Order> keywordContains(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            var user = root.join("user", JoinType.LEFT);
            return criteriaBuilder.or(criteriaBuilder.like(criteriaBuilder.lower(root.get("orderCode")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(user.get("username")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(user.get("email")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("shippingRecipientName")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("shippingPhoneNumber")), pattern));
        };
    }
}
