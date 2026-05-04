package com.event.eventservice.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AssignOrganizerRequest {

    private Long organizerId;
    private List<Long> organizerIds;

    public Long getOrganizerId() {
        return getOrganizerIds().isEmpty() ? null : getOrganizerIds().get(0);
    }

    public void setOrganizerId(Long organizerId) {
        this.organizerId = organizerId;
    }

    @NotNull
    public List<Long> getOrganizerIds() {
        if (organizerIds != null && !organizerIds.isEmpty()) {
            return organizerIds;
        }
        return organizerId == null ? List.of() : List.of(organizerId);
    }

    public void setOrganizerIds(List<Long> organizerIds) {
        this.organizerIds = organizerIds;
    }
}
