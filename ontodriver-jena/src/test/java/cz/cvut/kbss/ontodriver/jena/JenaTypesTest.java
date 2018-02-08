package cz.cvut.kbss.ontodriver.jena;

import cz.cvut.kbss.ontodriver.jena.environment.Generator;
import cz.cvut.kbss.ontodriver.jena.util.Procedure;
import cz.cvut.kbss.ontodriver.model.NamedResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static org.mockito.Mockito.when;

public class JenaTypesTest {

    private static final NamedResource SUBJECT = NamedResource.create(Generator.generateUri());

    @Mock
    private JenaAdapter adapterMock;

    @Mock
    private TypesHandler typesHandlerMock;

    @Mock
    private Procedure beforeMock;

    @Mock
    private Procedure afterMock;

    private JenaTypes types;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.types = new JenaTypes(adapterMock, beforeMock, afterMock);
        when(adapterMock.typesHandler()).thenReturn(typesHandlerMock);
    }

    @Test
    public void getTypesCallsBeforeHandlerBeforeGettingTypes() throws Exception {
        types.getTypes(SUBJECT, null, false);
        final InOrder inOrder = Mockito.inOrder(beforeMock, typesHandlerMock);
        inOrder.verify(beforeMock).execute();
        inOrder.verify(typesHandlerMock).getTypes(SUBJECT, null, false);
    }

    @Test
    public void addTypesCallsBeforeHandlerBeforeAddingTypes() throws Exception {
        final Set<URI> toAdd = Collections.singleton(Generator.generateUri());
        types.addTypes(SUBJECT, null, toAdd);
        final InOrder inOrder = Mockito.inOrder(beforeMock, typesHandlerMock);
        inOrder.verify(beforeMock).execute();
        inOrder.verify(typesHandlerMock).addTypes(SUBJECT, null, toAdd);
    }

    @Test
    public void addTypesCallsAfterHandlerAfterAddingTypes() throws Exception {
        final Set<URI> toAdd = Collections.singleton(Generator.generateUri());
        types.addTypes(SUBJECT, null, toAdd);
        final InOrder inOrder = Mockito.inOrder(afterMock, typesHandlerMock);
        inOrder.verify(typesHandlerMock).addTypes(SUBJECT, null, toAdd);
        inOrder.verify(afterMock).execute();
    }

    @Test
    public void removeTypesCallsBeforeHandlerBeforeRemovingTypes() throws Exception {
        final Set<URI> toRemove = Collections.singleton(Generator.generateUri());
        types.removeTypes(SUBJECT, null, toRemove);
        final InOrder inOrder = Mockito.inOrder(beforeMock, typesHandlerMock);
        inOrder.verify(beforeMock).execute();
        inOrder.verify(typesHandlerMock).removeTypes(SUBJECT, null, toRemove);
    }

    @Test
    public void removeTypesCallsAfterHandlerAfterRemovingTypes() throws Exception {
        final Set<URI> toRemove = Collections.singleton(Generator.generateUri());
        types.removeTypes(SUBJECT, null, toRemove);
        final InOrder inOrder = Mockito.inOrder(afterMock, typesHandlerMock);
        inOrder.verify(typesHandlerMock).removeTypes(SUBJECT, null, toRemove);
        inOrder.verify(afterMock).execute();
    }
}