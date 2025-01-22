package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.traccar.api.BaseResource;
import org.traccar.api.PaginatedResponse;
import org.traccar.dto.AddressDistanceDTO;
import org.traccar.model.Address;
import org.traccar.service.AddressService;
import org.traccar.api.security.UserPrincipal;
import org.traccar.storage.StorageException;

import java.util.logging.Logger;

import java.util.List;

@Path("/addresses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AddressResource extends BaseResource {

    @Inject
    private AddressService addressService;

    @Context
    private SecurityContext securityContext;


    @POST
    @Path(("/create"))
    public Response createAddress(Address address) {
        Address createdAddress = addressService.createAddress(address);
        return Response.status(Response.Status.CREATED).entity(createdAddress).build();
    }

    @POST
    @Path("/create-for-logged-user")
    public Response createAddressForLoggedUser(Address address) {
        long currentUserId = getCurrentUserId();

        address.setUserId(currentUserId);
        Address createdAddress = addressService.createAddress(address);
        return Response.status(Response.Status.CREATED).entity(createdAddress).build();
    }

    @GET
    @Path("/my")
    public Response getAddressesForCurrentUser(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("100") int size) {
        UserPrincipal principal = (UserPrincipal) securityContext.getUserPrincipal();
        long currentUserId = principal.getUserId();
        int offset = page * size;

        List<Address> addresses = addressService.getAddressesByUserId(currentUserId, offset, size);

        long totalCount = addressService.getTotalAddressCountForUser(currentUserId);

        PaginatedResponse paginatedResponse = new PaginatedResponse(addresses, totalCount, page, size);
        return Response.ok(paginatedResponse).build();
    }

    @GET
    @Path("/user/{userId}")
    public Response getAddressesForUser(
            @PathParam("userId") Long userId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("100") int size) {
        int offset = page * size;

        List<Address> addresses = addressService.getAddressesByUserId(userId, offset, size);

        long totalCount = addressService.getTotalAddressCountForUser(userId);

        PaginatedResponse paginatedResponse = new PaginatedResponse(addresses, totalCount, page, size);
        return Response.ok(paginatedResponse).build();
    }


    @GET
    @Path("/get/{id}")
    public Response getAddressById(@PathParam("id") Long id) {
        Address address = addressService.getAddressById(id);
        return Response.ok(address).build();
    }

    @GET
    @Path("/get/all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllAddresses(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("100") int size
    ) {
        List<Address> addresses = addressService.getAllAddresses(page, size);

        long totalCount = addressService.getTotalAddressCount();

        PaginatedResponse paginatedResponse = new PaginatedResponse(addresses, totalCount, page, size);

        return Response.ok(paginatedResponse).build();
    }

    @PUT
    @Path("/update/{id}")
    public Response updateAddress(@PathParam("id") Long id, Address updatedAddress) throws StorageException {
        long currentUserId = getCurrentUserId();

        if (!addressService.canUpdateAddress(currentUserId, id)) {
            throw new SecurityException("You do not have permission to update this address.");
        }

        Address address = addressService.updateAddress(id, updatedAddress);
        return Response.ok(address).build();
    }

    @DELETE
    @Path("/delete/{id}")
    public Response deleteAddress(@PathParam("id") Long id) throws StorageException {
        long currentUserId = getCurrentUserId();

        if (!addressService.canDeleteAddress(currentUserId, id)) {
            throw new SecurityException("You do not have permission to delete this address.");
        }
        addressService.deleteAddress(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/my/in-range")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAddressesInRangeForCurrentUser(
            @QueryParam("latitude") double latitude,
            @QueryParam("longitude") double longitude,
            @QueryParam("distanceKm") double distanceKm
    ) {
        Logger logger = Logger.getLogger(AddressResource.class.getName());
        long currentUserId = getCurrentUserId();

        logger.info(String.format("API called: GET /my/in-range?latitude=%.6f&longitude=%.6f&distanceKm=%.2f for userId=%d",
                latitude, longitude, distanceKm, currentUserId));

        List<AddressDistanceDTO> addresses = addressService.getAddressesWithinDistanceForUser(
                currentUserId, latitude, longitude, distanceKm);

        logger.info("API response: Returning " + addresses.size() + " addresses.");
        return Response.ok(addresses).build();
    }

    private long getCurrentUserId() {
        UserPrincipal principal = (UserPrincipal) securityContext.getUserPrincipal();
        return principal.getUserId();
    }
}


