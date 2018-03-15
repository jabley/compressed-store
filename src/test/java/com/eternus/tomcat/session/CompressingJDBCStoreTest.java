package com.eternus.tomcat.session;

import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.juli.logging.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

/**
 *
 */
public class CompressingJDBCStoreTest {

    private static final String CONNECTION_URL = "jdbc:h2:mem:test";

    private static final String DRIVER_NAME = "org.h2.Driver";

    private EmbeddedDatabase eb;


    @Before
    public void ensureDatabase() {
        eb = new EmbeddedDatabaseBuilder()
                .setName("test")
                .setType(H2)
                .setScriptEncoding("UTF-8")
                .ignoreFailedDrops(true)
                .addScript("schema-h2.sql")
                .build();
    }

    @After
    public void cleanupDatabase() {
        eb.shutdown();
    }

    private TestContext createTestContext() {
        CompressingJDBCStore store = new CompressingJDBCStore();

        Log log = mock(Log.class);

        Context context = mock(Context.class);
        when(context.getLogger()).thenReturn(log);
        when(context.getName()).thenReturn("my-servlet-context");

        final Manager manager = mock(Manager.class);
        when(manager.getContext()).thenReturn(context);
        when(manager.createEmptySession()).thenAnswer(invocation -> createSession(manager));
        when(manager.willAttributeDistribute(anyString(), any())).thenReturn(true);

        store.setManager(manager);

        store.setDriverName(DRIVER_NAME);
        store.setConnectionURL(CONNECTION_URL);
        store.setConnectionName("sa");
        store.setConnectionPassword("");

        return new TestContext(store, manager, context, log);
    }

    @Test
    public void emptyDatabaseHasNoSessions() throws IOException {
        TestContext tc = createTestContext();
        String[] keys = tc.store.keys();
        assertThat(keys, is(notNullValue()));
        assertThat(keys.length, is(0));

        assertThat(tc.store.getSize(), is(0));

        verify(tc.log, never()).error(any());
        verify(tc.log, never()).error(any(), any());
    }

    @Test
    public void canReadOurOwnWrites() throws IOException, ClassNotFoundException {
        TestContext tc = createTestContext();
        StandardSession session = createSession(tc.manager);
        Serializable foo = createGnarlyObjectGraph();
        session.setAttribute("foo", foo);
        tc.store.save(session);

        String[] keys = tc.store.keys();
        assertThat(keys, is(notNullValue()));
        assertThat(keys.length, is(1));

        assertThat(tc.store.getSize(), is(1));

        Session rehydrated = tc.store.load(session.getId());
        Object bar = ((HttpSession) rehydrated).getAttribute("foo");
        assertThat(foo, is(equalTo(bar)));

        verify(tc.log, never()).error(any());
        verify(tc.log, never()).error(any(), any());
    }

    private StandardSession createSession(Manager manager) {
        StandardSession session = new StandardSession(manager);
        session.setNew(true);
        session.setValid(true);
        session.setCreationTime(System.currentTimeMillis());
        session.setMaxInactiveInterval(60);
        session.setId(UUID.randomUUID().toString());
        return session;
    }

    private Serializable createGnarlyObjectGraph() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1024 * 1024 * 5; i++) {
            // predictable, repeating text like this should compress really well
            sb.appendCodePoint((byte) (i % 26) + 'a');
        }
        return sb.toString();
    }

    private static class TestContext {
        final CompressingJDBCStore store;

        final Manager manager;

        final Log log;

        final Context context;

        public TestContext(CompressingJDBCStore store, Manager manager, Context context, Log log) {
            this.store = store;
            this.manager = manager;
            this.context = context;
            this.log = log;
        }
    }
}
