package de.berlin.htw.control;

import de.berlin.htw.boundary.dto.Transaction;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {

    @Inject
    AccountService accountService;

    @GET
    @Path("/{accountId}/balance")
    public Response getBalance(@PathParam("accountId") String accountId) {
        double balance = accountService.getBalance(accountId);
        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("balance", balance);
        return Response.ok(response).build();
    }

    @GET
    @Path("/{accountId}/transactions/from")
    public Response getTransactionsFromAccount(@PathParam("accountId") String accountId) {
        List<Transaction> transactions = accountService.getTransactionsFromAccount(accountId);
        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("transactions", transactions);
        response.put("count", transactions.size());
        return Response.ok(response).build();
    }

    @GET
    @Path("/{accountId}/transactions/to")
    public Response getTransactionsToAccount(@PathParam("accountId") String accountId) {
        List<Transaction> transactions = accountService.getTransactionsToAccount(accountId);
        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("transactions", transactions);
        response.put("count", transactions.size());
        return Response.ok(response).build();
    }

    @GET
    @Path("/{accountId}/transactions")
    public Response getAllTransactionsForAccount(@PathParam("accountId") String accountId) {
        List<Transaction> transactions = accountService.getAllTransactionsForAccount(accountId);
        double balance = accountService.getBalance(accountId);
        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("balance", balance);
        response.put("transactions", transactions);
        response.put("count", transactions.size());
        return Response.ok(response).build();
    }

    @GET
    @Path("/transactions")
    public Response getAllTransactions() {
        List<Transaction> transactions = accountService.getAllTransactions();
        Map<String, Object> response = new HashMap<>();
        response.put("transactions", transactions);
        response.put("count", transactions.size());
        return Response.ok(response).build();
    }

}

