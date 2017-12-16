/**
 * Copyright (C) 2016 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jopa.owl2java;

import cz.cvut.kbss.jopa.owl2java.exception.OWL2JavaException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.vocab.XSDVocabulary;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static cz.cvut.kbss.jopa.owl2java.TestUtils.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

public class OWL2JavaTransformerTest {

    private static final String PACKAGE = "cz.cvut.kbss";

    // Thing is always generated by OWL2Java
    private static final List<String> KNOWN_CLASSES = Arrays
            .asList("Agent", "Person", "Organization", "Answer", "Question", "Report", "Thing");

    private String mappingFilePath;

    private OWLDataFactory dataFactory;

    private OWL2JavaTransformer transformer;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        this.mappingFilePath = resolveMappingFilePath();
        this.dataFactory = new OWLDataFactoryImpl();
        this.transformer = new OWL2JavaTransformer();
    }

    private String resolveMappingFilePath() {
        final File mf = new File(getClass().getClassLoader().getResource(MAPPING_FILE_NAME).getFile());
        return mf.getAbsolutePath();
    }

    @Test
    public void listContextsShowsContextsInICFile() {
        transformer.setOntology(IC_ONTOLOGY_IRI, mappingFilePath, true);
        final Collection<String> contexts = transformer.listContexts();
        assertEquals(1, contexts.size());
        assertEquals(CONTEXT, contexts.iterator().next());
    }

    @Test
    public void transformGeneratesJavaClassesFromIntegrityConstraints() throws Exception {
        final File targetDir = getTempDirectory();
        assertEquals(0, targetDir.listFiles().length);
        transformer.setOntology(IC_ONTOLOGY_IRI, mappingFilePath, true);
        transformer.transform(CONTEXT, "", targetDir.getAbsolutePath(), true);
        verifyGeneratedModel(targetDir);
    }

    @Test
    public void transformGeneratesJavaClassesInPackage() throws Exception {
        final String packageName = "cz.cvut.kbss.jopa.owl2java";
        final File targetDir = getTempDirectory();
        assertEquals(0, targetDir.listFiles().length);
        transformer.setOntology(IC_ONTOLOGY_IRI, mappingFilePath, true);
        transformer.transform(CONTEXT, packageName, targetDir.getAbsolutePath(), true);
        verifyGeneratedTree(packageName, targetDir);
    }

    private void verifyGeneratedTree(String packageName, File parentDir) {
        File currentDir = parentDir;
        for (String p : packageName.split("\\.")) {
            final List<String> files = Arrays.asList(currentDir.list());
            assertTrue(files.contains(p));
            currentDir = new File(currentDir.getAbsolutePath() + File.separator + p);
        }
        verifyVocabularyFileExistence(currentDir);
        verifyGeneratedModel(currentDir);
    }

    private void verifyGeneratedModel(File currentDir) {
        currentDir = new File(currentDir + File.separator + Constants.MODEL_PACKAGE);
        final List<String> classNames = Arrays.stream(currentDir.list())
                                              .map(fn -> fn.substring(0, fn.indexOf('.'))).collect(
                        Collectors.toList());
        assertTrue(classNames.containsAll(KNOWN_CLASSES));
    }

    @Test
    public void transformGeneratesVocabularyFile() throws Exception {
        final File targetDir = getTempDirectory();
        transformer.setOntology(IC_ONTOLOGY_IRI, mappingFilePath, true);
        transformer.transform(CONTEXT, "", targetDir.getAbsolutePath(), true);
        verifyVocabularyFileExistence(targetDir);
    }

    private void verifyVocabularyFileExistence(File targetDir) {
        final List<String> fileNames = Arrays.asList(targetDir.list());
        assertTrue(fileNames.contains(VOCABULARY_FILE));
    }

    @Test
    public void transformGeneratesVocabularyFileForTheWholeFile() throws Exception {
        final File targetDir = getTempDirectory();
        transformer.setOntology(IC_ONTOLOGY_IRI, mappingFilePath, true);
        transformer.transform(null, "", targetDir.getAbsolutePath(), true);
        verifyVocabularyFileExistence(targetDir);
    }

    @Test
    public void transformThrowsIllegalArgumentForUnknownContext() throws Exception {
        final String unknownContext = "someUnknownContext";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Context " + unknownContext + " not found.");
        final File targetDir = getTempDirectory();
        transformer.setOntology(IC_ONTOLOGY_IRI, mappingFilePath, true);
        transformer.transform(unknownContext, "", targetDir.getAbsolutePath(), true);
    }

    @Test
    public void setUnknownOntologyIriThrowsOWL2JavaException() {
        final String unknownOntoIri = "http://krizik.felk.cvut.cz/ontologies/an-unknown-ontology.owl";
        thrown.expect(OWL2JavaException.class);
        thrown.expectMessage("Unable to load ontology " + unknownOntoIri);
        transformer.setOntology(unknownOntoIri, mappingFilePath, true);
    }

    @Test
    public void setOntologyWithUnknownMappingFileThrowsIllegalArgument() {
        final String unknownMappingFile = "/tmp/unknown-mapping-file";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Mapping file " + unknownMappingFile + " not found.");
        transformer.setOntology(IC_ONTOLOGY_IRI, unknownMappingFile, true);
    }

    @Test
    public void generateVocabularyGeneratesOnlyVocabularyFile() throws Exception {
        final File targetDir = getTempDirectory();
        transformer.setOntology(IC_ONTOLOGY_IRI, mappingFilePath, true);
        transformer.generateVocabulary(CONTEXT, "", targetDir.getAbsolutePath(), false);
        final List<String> fileNames = Arrays.asList(targetDir.list());
        assertEquals(1, fileNames.size());
        assertEquals(VOCABULARY_FILE, fileNames.get(0));
    }

    @Test
    public void generateVocabularyGeneratesOnlyVocabularyFileForTheWholeFile() throws Exception {
        final File targetDir = getTempDirectory();
        transformer.setOntology(IC_ONTOLOGY_IRI, mappingFilePath, true);
        transformer.generateVocabulary(null, "", targetDir.getAbsolutePath(), false);
        final List<String> fileNames = Arrays.asList(targetDir.list());
        assertEquals(1, fileNames.size());
        assertEquals(VOCABULARY_FILE, fileNames.get(0));
    }

    @Test
    public void generateVocabularyTransformsInvalidCharactersInIrisToValid() throws Exception {
        final File targetDir = getTempDirectory();
        transformer.setOntology(IC_ONTOLOGY_IRI, mappingFilePath, true);
        // contains a ',', which will result in invalid Java identifier
        final String invalidIri = "http://onto.fel.cvut.cz/ontologies/aviation-safety/accident,_incident_or_emergency";
        final OWLAxiom axiom = dataFactory.getOWLDeclarationAxiom(dataFactory.getOWLClass(IRI.create(invalidIri)));
        TestUtils.addAxiom(axiom, transformer);

        transformer.generateVocabulary(null, "", targetDir.getAbsolutePath(), false);
        final File vocabularyFile = targetDir.listFiles()[0];
        final String fileContents = readFile(vocabularyFile);
        assertFalse(fileContents.contains(invalidIri.substring(invalidIri.lastIndexOf('/') + 1) + " ="));
        assertTrue(fileContents.contains(invalidIri.substring(invalidIri.lastIndexOf(',') + 1) + " ="));
    }

    private String readFile(File file) throws IOException {
        final StringBuilder sb = new StringBuilder();
        try (final BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    @Test
    public void transformGeneratesDPParticipationConstraintWithCorrectDatatypeIri() throws Exception {
        final File targetDir = getTempDirectory();
        assertEquals(0, targetDir.listFiles().length);
        transformer.setOntology(IC_ONTOLOGY_IRI, mappingFilePath, true);
        transformer.transform(CONTEXT, PACKAGE, targetDir.getAbsolutePath(), true);
        final List<String> generatedClass = getGeneratedClass(targetDir, "Answer");

        String fieldDeclaration = getFieldDeclaration(generatedClass, "hasValue");
        assertTrue(fieldDeclaration.contains(
                "@ParticipationConstraint(owlObjectIRI = \"" + XSDVocabulary.STRING.getIRI().toString() + "\""));
    }

    @Test
    public void transformGeneratesSubClass() throws Exception {
        final File targetDir = getTempDirectory();
        assertEquals(0, targetDir.listFiles().length);
        transformer.setOntology(IC_ONTOLOGY_IRI, mappingFilePath, true);
        transformer.transform(CONTEXT, PACKAGE, targetDir.getAbsolutePath(), true);
        final List<String> generatedClass = getGeneratedClass(targetDir, "Organization");

        final String classDeclaration = getExtendsClassDeclaration(generatedClass);
        assertTrue(classDeclaration.contains("extends Agent"));
    }

    private String getExtendsClassDeclaration(List<String> classFileLines) {
        int i;
        for (i = 0; i < classFileLines.size(); i++) {
            if (classFileLines.get(i).startsWith("public class")) {
                break;
            }
        }
        return classFileLines.get(i + 1);
    }

    @Test
    public void transformationFailsWhenImportCannotBeResolved() throws Exception {
        thrown.expect(OWL2JavaException.class);
        thrown.expectMessage(containsString("Unable to load ontology"));
        final File targetDir = getTempDirectory();
        transformer.setOntology(BAD_IMPORT_ONTOLOGY_IRI, mappingFilePath, true);
        transformer.generateVocabulary(null, "", targetDir.getAbsolutePath(), true);
    }

    @Test
    public void transformationIgnoresMissingImportWhenConfiguredTo() throws Exception {
        final File targetDir = getTempDirectory();
        transformer.ignoreMissingImports(true);
        transformer.setOntology(BAD_IMPORT_ONTOLOGY_IRI, mappingFilePath, true);
        transformer.generateVocabulary(null, "", targetDir.getAbsolutePath(), true);
        verifyVocabularyFileExistence(targetDir);
    }

    private List<String> getGeneratedClass(File directory, String className) throws Exception {
        final File path = new File(
                directory.getAbsolutePath() + File.separator + PACKAGE.replace(".", File.separator) + File.separator +
                        "model" + File.separator + className + ".java");
        return Files.readAllLines(path.toPath());
    }

    private String getFieldDeclaration(List<String> classFileLines, String fieldName) {
        int i;
        for (i = 0; i < classFileLines.size(); i++) {
            if (classFileLines.get(i).endsWith(fieldName + ";")) {
                break;
            }
        }
        int start = i - 1;
        while (start > 0 && !classFileLines.get(start).trim().startsWith("protected")) {
            start--;
        }
        final List<String> declaration = classFileLines.subList(start + 1, i + 1);
        return declaration.stream().reduce((a, b) -> a + b).get();
    }
}