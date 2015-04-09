package cz.cvut.kbss.jopa.test;

import cz.cvut.kbss.jopa.model.annotations.*;

import java.lang.reflect.Field;
import java.net.URI;

@OWLClass(iri = "http://krizik.felk.cvut.cz/ontologies/jopa/entities#OWLClassI")
public class OWLClassI {

	private static final String CLS_A_FIELD = "owlClassA";

	@Id
	private URI uri;

	@OWLObjectProperty(iri = "http://krizik.felk.cvut.cz/ontologies/jopa/attributes#hasA", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
	// @ParticipationConstraints({
	// @ParticipationConstraint(owlObjectIRI="http://new.owl#OWLClassA", min=1,
	// max=1)
	// })
	private OWLClassA owlClassA;

	/**
	 * @param uri
	 *            the uri to set
	 */
	public void setUri(URI uri) {
		this.uri = uri;
	}

	/**
	 * @return the uri
	 */
	public URI getUri() {
		return uri;
	}

	public void setOwlClassA(OWLClassA owlClassA) {
		this.owlClassA = owlClassA;
	}

	public OWLClassA getOwlClassA() {
		return owlClassA;
	}

	public static String getClassIri() {
		return OWLClassI.class.getAnnotation(OWLClass.class).iri();
	}
	
	public static Field getOwlClassAField() throws NoSuchFieldException, SecurityException {
		return OWLClassI.class.getDeclaredField(CLS_A_FIELD);
	}
}
