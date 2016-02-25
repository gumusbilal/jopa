/**
 * Copyright (C) 2016 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jopa.test.query;

import cz.cvut.kbss.jopa.model.EntityManager;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.descriptors.EntityDescriptor;
import cz.cvut.kbss.jopa.test.OWLClassA;
import cz.cvut.kbss.jopa.test.OWLClassB;
import cz.cvut.kbss.jopa.test.OWLClassD;
import cz.cvut.kbss.jopa.test.OWLClassE;
import cz.cvut.kbss.jopa.test.environment.TestEnvironmentUtils;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

public final class QueryTestEnvironment {

	private static final Logger LOG = Logger.getLogger(QueryTestEnvironment.class.getName());

	private static final String BASE_A = "http://krizik.felk.cvut.cz/ontologies/jopa/tests/entityA_";
	private static final String TYPE_A = "http://krizik.felk.cvut.cz/ontologies/jopa/entities#TypeA";
	private static final String BASE_B = "http://krizik.felk.cvut.cz/ontologies/jopa/tests/entityB_";
//	private static final String BASE_C = "http://krizik.felk.cvut.cz/ontologies/jopa/tests/entityC_";
	private static final String BASE_D = "http://krizik.felk.cvut.cz/ontologies/jopa/tests/entityD_";

	/**
	 * Default prefixes for SPARQL. </p>
	 * 
	 * Currently: owl, rdf, rdfs
	 */
	public static final String OWL_PREFIX = "PREFIX owl: <http://www.w3.org/2002/07/owl#>";
	public static final String RDF_PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>";
	public static final String RDFS_PREFIX = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>";

	/** owl:Thing class */
	public static final String OWL_THING = "http://www.w3.org/2002/07/owl#Thing";

	private static final URI NULL_CONTEXT = URI.create("http://NullContext");

	private static Map<Class<?>, List<?>> data;
	private static Map<URI, Map<Class<?>, List<?>>> dataByContext = new HashMap<>();

	private QueryTestEnvironment() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Generates and persists test data into the default context of the
	 * specified entity manager. </p>
	 * 
	 * @param em
	 *            EntityManager
	 */
	public static void generateTestData(EntityManager em) {
		assert em != null;
		final Map<Class<?>, List<?>> map = generate();
		LOG.config("Persisting test data...");
		persistIntoContext(em, map, null);
		data = map;
	}

	/**
	 * Generates and persists test data into the specified contexts. </p>
	 * 
	 * This method distributes the data approximately uniformly into all the
	 * specified contexts.
	 * 
	 * @param em
	 *            EntityManager
	 * @param contexts
	 *            A collection of target contexts
	 */
	public static void generateTestData(EntityManager em, Collection<URI> contexts) {
		assert em != null;
		assert contexts != null && !contexts.isEmpty();
		final Map<Class<?>, List<?>> map = generate();
		LOG.config("Persisting test data...");
		final int contextCount = contexts.size();
		final Map<URI, Map<Class<?>, List<?>>> contextMap = new HashMap<>();
		for (Entry<Class<?>, List<?>> e : map.entrySet()) {
			final List<?> dataLst = e.getValue();
			final int sublistSize = dataLst.size() / contextCount;
			int sublistStart = 0;
			for (URI ctx : contexts) {
				if (!contextMap.containsKey(ctx)) {
					contextMap.put(ctx, new HashMap<>());
				}
				final List<?> sublist = dataLst.subList(sublistStart, sublistStart + sublistSize);
				contextMap.get(ctx).put(e.getKey(), sublist);
				sublistStart += sublistSize;
			}
		}
		for (URI ctx : contextMap.keySet()) {
			persistIntoContext(em, contextMap.get(ctx), ctx);
		}
		data = map;
	}

	private static void persistIntoContext(EntityManager em, Map<Class<?>, List<?>> data,
			URI context) {
		final EntityDescriptor desc = new EntityDescriptor(context);
		em.getTransaction().begin();
		try {
			for (List<?> l : data.values()) {
				for (Object o : l) {
					em.persist(o, desc);
				}
			}
			em.getTransaction().commit();
			if (context == null) {
				context = NULL_CONTEXT;
			}
			dataByContext.put(context, data);
		} catch (RuntimeException e) {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
			throw e;
		}
	}

	/**
	 * Get all current test data.
	 * 
	 * @return
	 */
	public static Map<Class<?>, List<?>> getData() {
		return data;
	}

	/**
	 * Get a list of test instances of the specified class.
	 * 
	 * @param cls
	 *            The class
	 * @return List of test data of the specified class
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> getData(Class<T> cls) {
		assert cls != null;
		return (List<T>) data.get(cls);
	}

	/**
	 * Gets all data from the specified context. </p>
	 * 
	 * @param context
	 *            Context URI, null is permitted
	 * @return Map of all data or an empty map
	 */
	public static Map<Class<?>, List<?>> getDataByContext(URI context) {
		if (context == null) {
			context = NULL_CONTEXT;
		}
		if (!dataByContext.containsKey(context)) {
			return Collections.emptyMap();
		}
		return dataByContext.get(context);
	}

	/**
	 * Gets data from the specified context and of the specified type
	 * 
	 * @param context
	 *            Context URI, null is permitted
	 * @param cls
	 *            Data type
	 * @return List of data or an empty list
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> getDataByContext(URI context, Class<T> cls) {
		assert cls != null;
		if (context == null) {
			context = NULL_CONTEXT;
		}
		if (!dataByContext.containsKey(context)) {
			return Collections.emptyList();
		}
		final Map<Class<?>, List<?>> contextData = dataByContext.get(context);
		if (!contextData.containsKey(cls)) {
			return Collections.emptyList();
		}
		return (List<T>) contextData.get(cls);
	}

	private static Map<Class<?>, List<?>> generate() {
		LOG.config("Generating test data...");
		final Map<Class<?>, List<?>> m = new HashMap<>();
		final int count = 10;
		final OWLClass ann = OWLClassA.class.getAnnotation(OWLClass.class);
		final List<OWLClassA> aa = new ArrayList<>(count);
		m.put(OWLClassA.class, aa);
		int randomNum = TestEnvironmentUtils.randomInt(1000);
		for (int i = 0; i < count; i++) {
			final OWLClassA a = new OWLClassA();
			a.setUri(URI.create(BASE_A + randomNum));
			a.setStringAttribute("stringAttribute" + randomNum);
			final Set<String> s = new HashSet<>();
			s.add(TYPE_A);
			s.add(ann.iri());
			a.setTypes(s);
			aa.add(a);
			randomNum++;
		}
		final List<OWLClassB> bb = new ArrayList<>(count);
		m.put(OWLClassB.class, bb);
		randomNum = TestEnvironmentUtils.randomInt(1000);
		for (int i = 0; i < count; i++) {
			final OWLClassB b = new OWLClassB();
			b.setUri(URI.create(BASE_B + randomNum));
			b.setStringAttribute("strAtt" + randomNum);
			bb.add(b);
			randomNum++;
		}
		// final List<OWLClassC> cc = new ArrayList<>(count);
		// m.put(OWLClassC.class, cc);
		// randomNum = TestEnvironmentUtils.randomInt(1000);
		// for (int i = 0; i < count; i++) {
		// final OWLClassC c = new OWLClassC();
		// c.setUri(URI.create(BASE_C + randomNum));
		// if (i % 2 != 0) {
		// c.setReferencedList(new ArrayList<>(aa));
		// }
		// randomNum++;
		// cc.add(c);
		// }
		final List<OWLClassD> dd = new ArrayList<>();
		m.put(OWLClassD.class, dd);
		randomNum = TestEnvironmentUtils.randomInt(1000);
		for (int i = 0; i < count; i++) {
			final OWLClassD d = new OWLClassD();
			d.setUri(URI.create(BASE_D + randomNum));
			d.setOwlClassA(aa.get(i));
			dd.add(d);
			randomNum++;
		}
		final List<OWLClassE> ee = new ArrayList<>();
		m.put(OWLClassE.class, ee);
		for (int i = 0; i < count; i++) {
			final OWLClassE e = new OWLClassE();
			// Auto-generated id
			e.setStringAttribute("eStr");
			ee.add(e);
		}
		return m;
	}
}
