package cz.cvut.kbss.jopa.sessions.merge;

import cz.cvut.kbss.jopa.environment.OWLClassA;
import cz.cvut.kbss.jopa.environment.OWLClassD;
import cz.cvut.kbss.jopa.environment.utils.Generators;
import cz.cvut.kbss.jopa.environment.utils.MetamodelMocks;
import cz.cvut.kbss.jopa.model.MetamodelImpl;
import cz.cvut.kbss.jopa.model.descriptors.Descriptor;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.jopa.model.metamodel.EntityType;
import cz.cvut.kbss.jopa.model.metamodel.FieldSpecification;
import cz.cvut.kbss.jopa.sessions.UnitOfWorkImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ManagedTypeValueMergerTest {

    @Mock
    private UnitOfWorkImpl uow;

    @Mock
    private MetamodelImpl metamodel;

    private Descriptor descriptor;

    private FieldSpecification<? super OWLClassD, ?> refASpec;

    private ManagedTypeValueMerger merger;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final MetamodelMocks mocks = new MetamodelMocks();
        mocks.setMocks(metamodel);
        when(uow.getMetamodel()).thenReturn(metamodel);
        when(uow.isTypeManaged(any())).thenAnswer(invocation -> {
            final Class<?> arg = (Class<?>) invocation.getArguments()[0];
            return metamodel.entity(arg) != null;
        });

        final EntityType<OWLClassD> etD = metamodel.entity(OWLClassD.class);
        this.refASpec = etD.getFieldSpecification(OWLClassD.getOwlClassAField().getName());
        this.descriptor = new EntityDescriptor();
        this.merger = new ManagedTypeValueMerger(uow);
    }

    @Test
    public void mergeValueLoadsInstanceFromRepository() throws Exception {
        final OWLClassA orig = Generators.generateOwlClassAInstance();
        final OWLClassA merged = new OWLClassA(orig);
        final OWLClassD target = new OWLClassD(Generators.createIndividualIdentifier());
        when(uow.readObject(OWLClassA.class, merged.getUri(), descriptor)).thenReturn(orig);

        merger.mergeValue(refASpec, target, null, merged, descriptor);
        verify(uow).readObject(OWLClassA.class, merged.getUri(), descriptor);
        assertSame(orig, target.getOwlClassA());
    }

    @Test
    public void mergeValueSetsValueDirectlyWhenItIsNull() throws Exception {
        final OWLClassD target = new OWLClassD(Generators.createIndividualIdentifier());
        target.setOwlClassA(Generators.generateOwlClassAInstance());
        merger.mergeValue(refASpec, target, target.getOwlClassA(), null, descriptor);
        assertNull(target.getOwlClassA());
    }

    @Test
    public void mergeValueSetsTheMergedValueDirectlyWhenItRepresentsANewInstance() throws Exception {
        final OWLClassD target = new OWLClassD(Generators.createIndividualIdentifier());
        target.setOwlClassA(Generators.generateOwlClassAInstance());
        final OWLClassA merged = Generators.generateOwlClassAInstance();

        merger.mergeValue(refASpec, target, target.getOwlClassA(), merged, descriptor);
        assertSame(merged, target.getOwlClassA());
    }

    @Test
    public void mergeSetsTheMergedValueDirectlyWhenItHasNoIdentifier() throws Exception {
        final OWLClassD target = new OWLClassD(Generators.createIndividualIdentifier());
        target.setOwlClassA(Generators.generateOwlClassAInstance());
        final OWLClassA merged = Generators.generateOwlClassAInstance();
        merged.setUri(null);
        when(uow.readObject(any(), isNull(), any())).thenThrow(new NullPointerException());

        merger.mergeValue(refASpec, target, target.getOwlClassA(), merged, descriptor);
        assertSame(merged, target.getOwlClassA());
    }
}