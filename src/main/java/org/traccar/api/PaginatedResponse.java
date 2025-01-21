package org.traccar.api;

import org.traccar.model.Address;

import java.util.List;

public class PaginatedResponse {
    private List<Address> addresses;
    private long totalCount;
    private int currentPage;
    private int pageSize;

    public PaginatedResponse(List<Address> addresses, long totalCount, int currentPage, int pageSize) {
        this.addresses = addresses;
        this.totalCount = totalCount;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    // Getters and Setters
    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
