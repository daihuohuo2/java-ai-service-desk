package cn.liuyang.desk;

import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties = {
    "spring.datasource.url=${TEST_DATABASE_URL:jdbc:h2:mem:desk_tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${TEST_DATABASE_USER:sa}",
    "spring.datasource.password=${TEST_DATABASE_PASSWORD:}",
    "desk.demo=true", "desk.jobs.worker-enabled=false", "desk.cache.enabled=false", "desk.jobs.rabbit-enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class FullstackIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired Store store;
    @Autowired OrderService orders;
    @Autowired ProductService products;
    @Autowired ProductRepository productRepository;
    @Autowired IndexJobService jobs;
    @Autowired IndexJobProcessor processor;
    @Autowired TicketService tickets;
    @MockitoBean AiGateway ai;

    @BeforeEach void models() {
        when(ai.embed(anyString())).thenReturn(new float[]{1, 0, 0});
        when(ai.embeddingName()).thenReturn("test-embedding");
    }

    @Test void orderPageUsesTypedRowsAndPreservesWireNames() throws Exception {
        mvc.perform(get("/api/orders/page?size=1").with(user("customer").roles("CUSTOMER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.pages").value(2)).andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].order_no").value("ZS20261030001"));
    }
    @Test void secondOrderPageDoesNotRepeatRows() {
        assertNotEquals(orders.page("customer",1,1,null,null).items().getFirst().id(),
                orders.page("customer",2,1,null,null).items().getFirst().id());
    }
    @Test void orderStatusAndNumberFilterCombine() {
        assertEquals(1, orders.page("customer",1,10,"DELIVERED","30001").total());
        assertEquals(0, orders.page("customer",1,10,"SHIPPED","30001").total());
    }
    @Test void orderFiltersCannotRevealForeignOrders() {
        assertEquals(0, orders.page("customer",1,10,null,"30003").total());
    }
    @Test void queryWildcardsAreLiteral() {
        assertEquals(0, orders.page("customer",1,10,null,"%").total());
        assertEquals(0, orders.page("customer",1,10,null,"' OR 1=1 --").total());
    }
    @Test void invalidPaginationIsRejected() throws Exception {
        for (String query : List.of("page=0","size=101","page=abc","status=INVALID","page=1000001"))
            mvc.perform(get("/api/orders/page?" + query).with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isBadRequest());
    }
    @Test void beyondLastPageIsEmptyButKeepsTotal() {
        var result = orders.page("customer",100,10,null,null);
        assertEquals(2,result.total()); assertTrue(result.items().isEmpty());
    }
    @Test void productSaveAdvancesRevisionAndPreservesOrderPrice() {
        long before=productRepository.revision();
        products.save("P100",new ProductInput("新版阅读灯","照明",99999,"新的商品描述",true),"admin");
        assertEquals(before+1,productRepository.revision());
        assertEquals(19900,orders.owned("customer","order-1").totalCents());
    }
    @Test void inactiveProductNotInPublicCatalog() {
        products.save("P100",new ProductInput("阅读灯","照明",19900,"说明",false),"admin");
        assertTrue(products.active().stream().noneMatch(p->p.id().equals("P100")));
        assertTrue(products.all().stream().anyMatch(p->p.id().equals("P100")));
    }
    @Test void missingProductDoesNotAdvanceRevision() {
        long before=productRepository.revision();
        assertThrows(ApiException.class,()->products.save("missing",new ProductInput("名称","分类",1,"说明",true),"admin"));
        assertEquals(before,productRepository.revision());
    }
    @Test void customerCannotModifyProducts() throws Exception {
        mvc.perform(post("/api/admin/products").with(user("customer").roles("CUSTOMER")).with(csrf())
            .contentType("application/json").content("{}")).andExpect(status().isForbidden());
    }
    @Test void negativeProductPriceRejected() throws Exception {
        mvc.perform(post("/api/admin/products").with(user("admin").roles("ADMIN")).with(csrf())
            .contentType("application/json").content(store.encode(new ProductInput("名称","分类",-1,"说明",true))))
            .andExpect(status().isBadRequest());
    }
    @Test void ticketPaginationIsScopedToCustomer() throws Exception {
        mvc.perform(get("/api/tickets/page").with(user("customer2").roles("CUSTOMER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }
    @Test void ticketFiltersAreExecutedOnServer() throws Exception {
        mvc.perform(get("/api/tickets/page?status=IN_PROGRESS&query=物流").with(user("agent").roles("AGENT")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].ticket_no").value("TK-DEMO-001"));
    }
    @Test void exportHidesAnotherCustomersTickets() throws Exception {
        String csv=mvc.perform(get("/api/tickets/export").with(user("customer2").roles("CUSTOMER")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(csv.contains("TK-DEMO-001"));
    }
    @Test void exportEscapesSpreadsheetFormulasAndQuotes() throws Exception {
        tickets.create("customer",null,"QUESTION","=HYPERLINK(\"example\")","描述","csv-formula-test");
        String csv=mvc.perform(get("/api/tickets/export?query=HYPERLINK").with(user("customer").roles("CUSTOMER")))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.contains("'=HYPERLINK("));
        assertTrue(csv.contains("\"\"example\"\""));
    }
    @Test void invalidTicketStateRejected() throws Exception {
        mvc.perform(get("/api/tickets/page?status=INVALID").with(user("customer").roles("CUSTOMER")))
            .andExpect(status().isBadRequest());
    }
    @Test void customerCannotSeeIndexJobs() throws Exception {
        mvc.perform(get("/api/knowledge/jobs").with(user("customer").roles("CUSTOMER")))
            .andExpect(status().isForbidden());
    }
    @Test void agentCannotSubmitIndexJobs() throws Exception {
        mvc.perform(post("/api/knowledge/jobs").with(user("agent").roles("AGENT")).with(csrf())
            .header("Idempotency-Key","job-agent-test").contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
    }
    @Test void administratorSubmitsJobWithoutRunningModelInline() throws Exception {
        mvc.perform(post("/api/knowledge/jobs").with(user("admin").roles("ADMIN")).with(csrf())
            .header("Idempotency-Key","job-admin-test").contentType("application/json").content("{}"))
            .andExpect(status().isAccepted()).andExpect(jsonPath("$.state").value("PENDING"));
        verify(ai,never()).embed(anyString());
    }
    @Test void duplicateJobSubmissionReusesId() {
        assertEquals(jobs.submit(null,"admin","same-job-key").id(),jobs.submit(null,"admin","same-job-key").id());
    }
    @Test void reusedJobKeyCannotChangeDocument() {
        jobs.submit(null,"admin","same-job-key");
        assertEquals(409,assertThrows(ApiException.class,()->jobs.submit("doc-lamp","admin","same-job-key")).status);
    }
    @Test void invalidDocumentRejectedBeforeJobInsertion() {
        assertEquals(404,assertThrows(ApiException.class,()->jobs.submit("missing","admin","invalid-doc-key")).status);
    }
    @Test void completedJobIsNotExecutedTwice() {
        var job=jobs.submit("doc-lamp","admin","complete-job-key");
        processor.process(job.id()); processor.process(job.id());
        assertEquals("SUCCEEDED",jobs.get(job.id()).state());
        assertEquals(1,jobs.get(job.id()).attempts());
        verify(ai,times(3)).embed(anyString());
    }
    @Test void failedEmbeddingsRetryThenStop() {
        when(ai.embed(anyString())).thenReturn(null);
        var job=jobs.submit("doc-lamp","admin","retry-job-key");
        for(int attempt=1;attempt<=3;attempt++) {
            store.db.update("UPDATE index_jobs SET available_at=0 WHERE id=?",job.id());
            processor.process(job.id());
            assertEquals(attempt,jobs.get(job.id()).attempts());
            assertEquals(attempt==3?"FAILED":"RETRY",jobs.get(job.id()).state());
        }
    }
    @Test void futureRetryIsNotClaimedEarly() {
        var job=jobs.submit(null,"admin","future-job-key");
        store.db.update("UPDATE index_jobs SET available_at=? WHERE id=?",System.currentTimeMillis()+60000,job.id());
        processor.process(job.id());
        assertEquals(0,jobs.get(job.id()).attempts());
    }
    @Test void liveLeasePreventsDuplicateConsumer() {
        var job=jobs.submit(null,"admin","live-lease-key");
        store.db.update("UPDATE index_jobs SET state='RUNNING',lease_until=? WHERE id=?",System.currentTimeMillis()+60000,job.id());
        processor.process(job.id());
        verify(ai,never()).embed(anyString());
    }
    @Test void expiredWorkerLeaseIsRecovered() {
        var job=jobs.submit("doc-lamp","admin","expired-lease-key");
        store.db.update("UPDATE index_jobs SET state='RUNNING',lease_until=0,attempts=1 WHERE id=?",job.id());
        processor.process(job.id());
        assertEquals("SUCCEEDED",jobs.get(job.id()).state());
        assertEquals(2,jobs.get(job.id()).attempts());
    }
    @Test void failedJobCanBeRetriedAfterModelRecovery() {
        var job=jobs.submit("doc-lamp","admin","manual-retry-key");
        store.db.update("UPDATE index_jobs SET state='FAILED',attempts=3 WHERE id=?",job.id());
        jobs.retry(job.id(),"admin"); processor.process(job.id());
        assertEquals("SUCCEEDED",jobs.get(job.id()).state());
    }
    @Test void successfulJobCannotBeRetriedAccidentally() {
        var job=jobs.submit("doc-lamp","admin","no-retry-key");
        processor.process(job.id());
        assertEquals(409,assertThrows(ApiException.class,()->jobs.retry(job.id(),"admin")).status);
    }
    @Test void invalidForeignKeyIsRejectedByDatabase() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->store.db.update(
            "INSERT INTO shop_orders(id,order_no,owner_id,product_id,quantity,total_cents,status,created_at) VALUES(?,?,?,?,?,?,?,?)",
            "bad-order","ZS99999999","not-a-user","P100",1,1,"PAID",Store.now()));
    }
    @Test void staleWorkerCannotCompleteAfterLosingItsLeaseToken() {
        var job=jobs.submit("doc-lamp","admin","fencing-token-key");
        when(ai.embed(anyString())).thenAnswer(invocation -> {
            store.db.update("UPDATE index_jobs SET lease_token=? WHERE id=?", "new-worker-token", job.id());
            return new float[]{1,0,0};
        });
        processor.process(job.id());
        assertEquals("RUNNING",jobs.get(job.id()).state());
        assertEquals("new-worker-token",store.one("SELECT lease_token FROM index_jobs WHERE id=?",job.id()).get("lease_token"));
    }
}
