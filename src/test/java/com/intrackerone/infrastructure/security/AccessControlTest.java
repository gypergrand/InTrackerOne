package com.intrackerone.infrastructure.security;

import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessControlTest {

    private final User owner = User.builder().id(1L).build();
    private final User otherUser = User.builder().id(2L).build();

    // ========== hasGlobalDataAccess ==========

    @Test
    void hasGlobalDataAccess_admin_returnsTrue() {
        assertThat(AccessControl.hasGlobalDataAccess(Role.ADMIN)).isTrue();
    }

    @Test
    void hasGlobalDataAccess_moderator_returnsTrue() {
        assertThat(AccessControl.hasGlobalDataAccess(Role.MODERATOR)).isTrue();
    }

    @Test
    void hasGlobalDataAccess_user_returnsFalse() {
        assertThat(AccessControl.hasGlobalDataAccess(Role.USER)).isFalse();
    }

    // ========== requireOwnerOrGlobal (User owner) ==========

    @Test
    void requireOwnerOrGlobal_admin_allows() {
        AccessControl.requireOwnerOrGlobal(owner, 99L, Role.ADMIN, "Company");
    }

    @Test
    void requireOwnerOrGlobal_moderator_allows() {
        AccessControl.requireOwnerOrGlobal(owner, 99L, Role.MODERATOR, "Company");
    }

    @Test
    void requireOwnerOrGlobal_userOwnsResource_allows() {
        AccessControl.requireOwnerOrGlobal(owner, 1L, Role.USER, "Company");
    }

    @Test
    void requireOwnerOrGlobal_userDoesNotOwn_throws() {
        assertThatThrownBy(() -> AccessControl.requireOwnerOrGlobal(owner, 2L, Role.USER, "JobApplication"))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("JobApplication");
    }

    @Test
    void requireOwnerOrGlobal_nullOwner_user_throws() {
        assertThatThrownBy(() -> AccessControl.requireOwnerOrGlobal((User) null, 2L, Role.USER, "Resource"))
            .isInstanceOf(UnauthorizedException.class);
    }

    // ========== requireOwnerOrGlobal (Long ownerId) ==========

    @Test
    void requireOwnerOrGlobal_byId_admin_allows() {
        AccessControl.requireOwnerOrGlobal(1L, 99L, Role.ADMIN, "Resource");
    }

    @Test
    void requireOwnerOrGlobal_byId_userOwns_allows() {
        AccessControl.requireOwnerOrGlobal(1L, 1L, Role.USER, "Resource");
    }

    @Test
    void requireOwnerOrGlobal_byId_userDoesNotOwn_throws() {
        assertThatThrownBy(() -> AccessControl.requireOwnerOrGlobal(1L, 2L, Role.USER, "Resource"))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void requireOwnerOrGlobal_byId_nullOwnerId_user_throws() {
        assertThatThrownBy(() -> AccessControl.requireOwnerOrGlobal((Long) null, 2L, Role.USER, "Resource"))
            .isInstanceOf(UnauthorizedException.class);
    }

    // ========== requireSameOwner ==========

    @Test
    void requireSameOwner_nullActualOwner_allows() {
        AccessControl.requireSameOwner(owner, null, "PipelineStatus");
    }

    @Test
    void requireSameOwner_sameOwner_allows() {
        AccessControl.requireSameOwner(owner, owner, "Resource");
    }

    @Test
    void requireSameOwner_differentOwner_throws() {
        assertThatThrownBy(() -> AccessControl.requireSameOwner(owner, otherUser, "Project"))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("belongs to another user");
    }

    @Test
    void requireSameOwner_bothNull_allows() {
        AccessControl.requireSameOwner(null, null, "Resource");
    }

    // ========== isOwner ==========

    @Test
    void isOwner_whenSameUser_returnsTrue() {
        assertThat(AccessControl.isOwner(owner, 1L)).isTrue();
    }

    @Test
    void isOwner_whenDifferentUser_returnsFalse() {
        assertThat(AccessControl.isOwner(owner, 2L)).isFalse();
    }

    @Test
    void isOwner_nullOwner_returnsFalse() {
        assertThat(AccessControl.isOwner(null, 1L)).isFalse();
    }

    @Test
    void isOwner_nullOwnerId_returnsFalse() {
        User userWithNullId = User.builder().id(null).build();
        assertThat(AccessControl.isOwner(userWithNullId, 1L)).isFalse();
    }
}
