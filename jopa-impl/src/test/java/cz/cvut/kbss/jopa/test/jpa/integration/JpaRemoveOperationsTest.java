package cz.cvut.kbss.jopa.test.jpa.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.test.OWLClassA;
import cz.cvut.kbss.jopa.test.OWLClassD;
import cz.cvut.kbss.jopa.test.OWLClassG;
import cz.cvut.kbss.jopa.test.OWLClassH;
import cz.cvut.kbss.jopa.test.OWLClassI;
import cz.cvut.kbss.jopa.test.TestEnvironment;
import cz.cvut.kbss.jopa.test.utils.JenaStorageConfig;
import cz.cvut.kbss.jopa.test.utils.OwlapiStorageConfig;
import cz.cvut.kbss.jopa.test.utils.OwldbStorageConfig;
import cz.cvut.kbss.jopa.test.utils.StorageConfig;
import cz.cvut.kbss.ontodriver.Context;

public class JpaRemoveOperationsTest {

	private static final Logger LOG = Logger.getLogger(JpaRemoveOperationsTest.class.getName());

	private static final List<StorageConfig> storages = initStorages();

	private static OWLClassA entityA;
	private static OWLClassD entityD;
	private static OWLClassG entityG;
	private static OWLClassH entityH;
	private static OWLClassI entityI;

	private static EntityManager em;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		entityA = new OWLClassA();
		entityA.setUri(URI.create("http://krizik.felk.cvut.cz/ontologies/jopa/tests/entityA"));
		entityA.setStringAttribute("entityAStringAttribute");
		final Set<String> types = new HashSet<String>();
		types.add("OWLClassA");
		entityA.setTypes(types);
		entityD = new OWLClassD();
		entityD.setUri(URI.create("http://krizik.felk.cvut.cz/ontologies/jopa/tests/entityD"));
		entityD.setOwlClassA(entityA);
		entityI = new OWLClassI();
		entityI.setUri(URI.create("http://krizik.felk.cvut.cz/ontologies/jopa/tests/entityI"));
		entityI.setOwlClassA(entityA);
		entityH = new OWLClassH();
		entityH.setUri(URI.create("http://krizik.felk.cvut.cz/ontologies/jopa/tests/entityH"));
		entityH.setOwlClassA(entityA);
		entityG = new OWLClassG();
		entityG.setUri(URI.create("http://krizik.felk.cvut.cz/ontologies/jopa/tests/entityG"));
		entityG.setOwlClassH(entityH);
	}

	@Before
	public void setUp() throws Exception {
		TestEnvironment.clearDatabase();
		TestEnvironment.resetOwldbHibernateProvider();
	}

	@After
	public void tearDown() throws Exception {
		if (em.isOpen()) {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
			em.close();
			em.getEntityManagerFactory().close();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemoveNotManaged() {
		LOG.config("Test: removed not managed entity.");
		em = TestEnvironment.getPersistenceConnector("RemoveNotManaged", storages, false);
		em.remove(entityA);
		fail("This line should not have been reached.");
	}

	@Test(expected = NullPointerException.class)
	public void testRemoveNull() {
		LOG.config("Test: remove null.");
		em = TestEnvironment.getPersistenceConnector("RemoveNull", storages, false);
		em.remove(null);
		fail("This line should not have been reached.");
	}

	@Test
	public void testRemoveFromOne() {
		LOG.config("Test: remove entity from one context.");
		em = TestEnvironment.getPersistenceConnector("RemoveFromOneContext", storages, false);
		final List<Context> contexts = em.getAvailableContexts();
		em.getTransaction().begin();
		for (Context ctx : contexts) {
			em.persist(entityA, ctx.getUri());
		}
		em.getTransaction().commit();
		final OWLClassA toRemove = em.find(OWLClassA.class, entityA.getUri());
		assertNotNull(toRemove);
		final OWLClassA nextOne = em.find(OWLClassA.class, entityA.getUri(),
				contexts.get(contexts.size() - 1).getUri());
		assertNotNull(nextOne);
		em.getTransaction().begin();
		em.remove(toRemove);
		assertFalse(em.contains(toRemove));
		assertTrue(em.contains(nextOne));
		em.getTransaction().commit();
		final OWLClassA res = em.find(OWLClassA.class, entityA.getUri());
		assertNotNull(res);
	}

	@Test
	public void testRemoveFromRelationship() {
		LOG.config("Test: remove owner of a relationship.");
		em = TestEnvironment.getPersistenceConnector("RemoveRelationshipOwner", storages, true);
		em.getTransaction().begin();
		em.persist(entityD);
		em.persist(entityA);
		em.getTransaction().commit();
		em.clear();

		final OWLClassD d = em.find(OWLClassD.class, entityD.getUri());
		assertNotNull(d);
		assertTrue(em.contains(d));
		final OWLClassA a = d.getOwlClassA();
		assertTrue(em.contains(a));
		em.getTransaction().begin();
		em.remove(d);
		assertTrue(em.contains(a));
		assertFalse(em.contains(d));
		em.getTransaction().commit();

		final OWLClassD resD = em.find(OWLClassD.class, entityD.getUri());
		assertNull(resD);
		final OWLClassA resA = em.find(OWLClassA.class, entityA.getUri());
		assertNotNull(resA);
		assertEquals(entityA.getStringAttribute(), resA.getStringAttribute());
	}

	@Test
	public void testRemoveCascade() {
		LOG.config("Test: remove with cascade.");
		em = TestEnvironment.getPersistenceConnector("RemoveCascade", storages, false);
		em.getTransaction().begin();
		final Context ctx = em.getAvailableContexts().get(em.getAvailableContexts().size() - 1);
		em.persist(entityH, ctx.getUri());
		assertTrue(em.contains(entityA));
		em.getTransaction().commit();
		em.clear();

		final OWLClassH toRemove = em.find(OWLClassH.class, entityH.getUri(), ctx.getUri());
		assertNotNull(toRemove);
		assertNotNull(toRemove.getOwlClassA());
		assertTrue(em.contains(toRemove.getOwlClassA()));
		em.getTransaction().begin();
		em.remove(toRemove);
		assertFalse(em.contains(toRemove));
		assertFalse(em.contains(toRemove.getOwlClassA()));
		em.getTransaction().commit();

		final OWLClassH resH = em.find(OWLClassH.class, entityH.getUri(), ctx.getUri());
		assertNull(resH);
		final OWLClassA resA = em.find(OWLClassA.class, entityA.getUri(), ctx.getUri());
		assertNull(resA);
	}

	@Test
	public void testRemoveBeforeCommit() {
		LOG.config("Test: persist entity and remove it before transaction commit.");
		em = TestEnvironment.getPersistenceConnector("RemoveBeforeCommit", storages, true);
		em.getTransaction().begin();
		em.persist(entityA);
		assertTrue(em.contains(entityA));
		em.remove(entityA);
		assertFalse(em.contains(entityA));
		em.getTransaction().commit();

		final OWLClassA res = em.find(OWLClassA.class, entityA.getUri());
		assertNull(res);
	}

	private static List<StorageConfig> initStorages() {
		final List<StorageConfig> lst = new ArrayList<>(3);
		lst.add(new OwlapiStorageConfig());
		lst.add(new OwldbStorageConfig());
		lst.add(new JenaStorageConfig());
		return lst;
	}
}