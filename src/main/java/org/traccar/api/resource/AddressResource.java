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
import org.traccar.model.Address;
import org.traccar.service.AddressService;
import org.traccar.api.security.UserPrincipal;


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
        // Extract user ID from the principal:
        UserPrincipal principal = (UserPrincipal) securityContext.getUserPrincipal();
        long currentUserId = principal.getUserId();

        // Assign userId to the address:
        address.setUserId(currentUserId);

        // Now create the address
        Address createdAddress = addressService.createAddress(address);
        return Response.status(Response.Status.CREATED).entity(createdAddress).build();
    }

    @GET
    @Path("/my")
    public Response getAddressesForCurrentUser() {
        UserPrincipal principal = (UserPrincipal) securityContext.getUserPrincipal();
        long currentUserId = principal.getUserId();

        List<Address> addresses = addressService.getAddressesByUserId(currentUserId);
        return Response.ok(addresses).build();
    }

    @GET
    @Path("/user/{userId}")
    public Response getAddressesForUser(@PathParam("userId") Long userId) {
        List<Address> addresses = addressService.getAddressesByUserId(userId);
        return Response.ok(addresses).build();
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
            @QueryParam("size") @DefaultValue("10") int size
    ) {
        List<Address> addresses = addressService.getAllAddresses(page, size);

        long totalCount = addressService.getTotalAddressCount();

        PaginatedResponse paginatedResponse = new PaginatedResponse(addresses, totalCount, page, size);

        return Response.ok(paginatedResponse).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateAddress(@PathParam("id") Long id, Address updatedAddress) {
        Address address = addressService.updateAddress(id, updatedAddress);
        return Response.ok(address).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteAddress(@PathParam("id") Long id) {
        addressService.deleteAddress(id);
        return Response.noContent().build();
    }
}


