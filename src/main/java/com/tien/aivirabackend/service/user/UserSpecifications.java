package com.tien.aivirabackend.service.user;

import jakarta.persistence.criteria.JoinType;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.domain.entity.user.User;

@Component
public class UserSpecifications {
    public Specification<User> adminUsers(
            String keyword, PredefinedRole role, Boolean active, Boolean locked, Boolean emailVerified) {
        return Specification.where(keywordContains(keyword))
                .and(hasRole(role))
                .and(hasActive(active))
                .and(hasLocked(locked))
                .and(hasEmailVerified(emailVerified));
    }

    private Specification<User> keywordContains(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("id")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNumber")), pattern));
        };
    }

    private Specification<User> hasRole(PredefinedRole role) {
        return (root, query, criteriaBuilder) -> {
            if (role == null) {
                return null;
            }
            if (query != null) {
                query.distinct(true);
            }
            var roles = root.join("roles", JoinType.INNER);
            return criteriaBuilder.equal(roles.get("code"), role);
        };
    }

    private Specification<User> hasActive(Boolean active) {
        return (root, query, criteriaBuilder) ->
                active == null ? null : criteriaBuilder.equal(root.get("isActive"), active);
    }

    private Specification<User> hasLocked(Boolean locked) {
        return (root, query, criteriaBuilder) ->
                locked == null ? null : criteriaBuilder.equal(root.get("isLocked"), locked);
    }

    private Specification<User> hasEmailVerified(Boolean emailVerified) {
        return (root, query, criteriaBuilder) ->
                emailVerified == null ? null : criteriaBuilder.equal(root.get("emailVerified"), emailVerified);
    }
}
