package cz.cvut.kbss.jopa.test.query.owlapi;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.test.environment.OwlapiPersistenceFactory;
import cz.cvut.kbss.jopa.test.query.QueryTestEnvironment;
import cz.cvut.kbss.jopa.test.query.runner.PolymorphicSelectQueryRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class PolymorphicSelectQueryTest extends PolymorphicSelectQueryRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PolymorphicSelectQueryTest.class);

    private static EntityManager em;

    public PolymorphicSelectQueryTest() {
        super(LOG);
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final OwlapiPersistenceFactory persistenceFactory = new OwlapiPersistenceFactory();
        em = persistenceFactory.getEntityManager("PolymorphicSelectQueryTests", false, Collections.emptyMap());
        QueryTestEnvironment.generateTestData(em);
        em.clear();
        em.getEntityManagerFactory().getCache().evictAll();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        em.close();
        em.getEntityManagerFactory().close();
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Ignore
    @Test
    public void selectLoadsInstanceOfMostConcreteSubclassOfAbstractEntity() {
        // Another possible bug in OWL2Query - This query returns no results and there is a warning:
        // cz.cvut.kbss.owl2query.engine.QueryImpl checkType WARNING: 'rdfs:label' is not an object of type 'DATA_PROPERTY'.
    }

    @Ignore
    @Test
    public void selectLoadsInstanceOfMostConcreteSubclassOfConcreteEntity() {
        // Same as above
    }
}
