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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.BaseResource;
import org.traccar.model.Address;
import org.traccar.service.AddressService;

import java.util.List;


@Path("/addresses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AddressResource extends BaseResource {

    @Inject
    private AddressService addressService;


    @POST
    public Response createAddress(Address address) {
        Address createdAddress = addressService.createAddress(address);
        return Response.status(Response.Status.CREATED).entity(createdAddress).build();
    }

    @GET
    @Path("/{id}")
    public Response getAddressById(@PathParam("id") Long id) {
        Address address = addressService.getAddressById(id);
        return Response.ok(address).build();
    }

    @GET
    public Response getAllAddresses() {
        List<Address> addresses = addressService.getAllAddresses();
        return Response.ok(addresses).build();
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


