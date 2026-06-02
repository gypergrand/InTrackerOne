package com.intrackerone.infrastructure.security;

import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.exception.UnauthorizedException;

public final class AccessControl {

    private AccessControl() {
    }

    public static boolean hasGlobalDataAccess(Role role) {
        return role == Role.ADMIN || role == Role.MODERATOR;
    }

    public static void requireOwnerOrGlobal(User owner, Long currentUserId, Role role, String resource) {
        if (hasGlobalDataAccess(role) || isOwner(owner, currentUserId)) {
            return;
        }
        throw new UnauthorizedException("Access denied to " + resource);
    }

    public static void requireOwnerOrGlobal(Long ownerId, Long currentUserId, Role role, String resource) {
        if (hasGlobalDataAccess(role) || (ownerId != null && ownerId.equals(currentUserId))) {
            return;
        }
        throw new UnauthorizedException("Access denied to " + resource);
    }

    public static void requireSameOwner(User expectedOwner, User actualOwner, String resource) {
        if (sameOwner(expectedOwner, actualOwner)) {
            return;
        }
        throw new UnauthorizedException(resource + " belongs to another user");
    }

    public static boolean isOwner(User owner, Long currentUserId) {
        return owner != null && owner.getId() != null && owner.getId().equals(currentUserId);
    }

    private static boolean sameOwner(User first, User second) {
        return first != null && second != null && first.getId() != null && first.getId().equals(second.getId());
    }
}
