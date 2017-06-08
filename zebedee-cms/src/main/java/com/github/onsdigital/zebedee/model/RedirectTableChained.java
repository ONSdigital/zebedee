package com.github.onsdigital.zebedee.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * RedirectTable sits at the content level.
 *
 * <p>
 * <code>Chains</code>
 * <p>
 * RedirectTable supports chains of redirects. These can chain
 *
 * <p>
 *
 *
 * <code>Child</code><p>
 * The property <code>child</code> is referenced if the parent cannot resolve a uri.
 * <code>Child</code> may point at its own content or may have its own.
 * <p>
 *
 * Zebedee implementation
 *
 * Zebedee.published.redirect is the master table
 * Collections also have a cascaded redirect of <code>inProgress->complete->reviewed->published</code>
 *
 * This allows us to work with moves as something that can be published since inProgress.redirect is referenced before
 * we drop to the actual files.
 *
 * NOTE: This is our original redirect table. The intention is to
 *
 * TODO: Conceptual work regarding how a move collection can function
 * TODO: I can't visualise it working as an extension of the current florence interface
 *
 * Scenarios to consider: We want to
 */
public class RedirectTableChained {
    public static long timeInRedirect = 0;
    public static long getCalls = 0;

    private HashMap<String, String> table = new HashMap<>();
    private RedirectTableChained child = null;
    protected Content content = null;
    private final int ITERATION_MAX = 100; // Simple method to avoid cycling

    public RedirectTableChained(Content content) {
        this.content = content;
    }

    public RedirectTableChained(Content content, Path path) {
        this(content);
        try {
            load(path);
        } catch (IOException e) {
            logError(e, "Could not load redirect").addParameter("path", path.toString()).log();
        }
    }

    /**
     * Child allows us to chain redirect tables
     *
     * This allow with redirects in Florence since we can have an 'inprogress' table (the parent)
     * in tandem a 'published' table (the child)
     *
     * During publication we can merge chained redirects
     *
     * @param child a secondary
     */
    public void setChild(RedirectTableChained child) {
        this.child = child;
    }

    public RedirectTableChained getChild() {
        return child;
    }

    /**
     *
     * @param redirectFrom original uri
     * @param redirectTo redirect uri
     */
    public void addRedirect(String redirectFrom, String redirectTo) {
        table.put(redirectFrom, redirectTo);
    }

    public void removeRedirect(String redirectFrom) {
        table.remove(redirectFrom);
    }

    /**
     *
     * @param uri the requested uri
     *
     * @return the redirected uri
     */
    public String get(String uri) {

        String finalUriAtThisLevel = endChain(uri, ITERATION_MAX);        // Follow redirect chain
        if (finalUriAtThisLevel == null) { return null; }       // Check for cyclicality

        if (content.exists(finalUriAtThisLevel, false)) {       // Option 1) Uri exists - return it

            return finalUriAtThisLevel;
        } else if (child != null) {                             // Option 2) Child can continue the chain

            return child.get(finalUriAtThisLevel);
        } else {                                                // Option 3) Return null

            return null;
        }
    }

    private String endChain(String uri, int iterations) {
        if (iterations == 0) {
            return null;
        } // checks we haven't cycled

        if (!content.exists(uri, false) && table.containsKey(uri)) {
            return endChain(table.get(uri), --iterations);
        }
        return uri;
    }

    public void save(Path path) throws IOException {
        // TODO quite a considerable amount of threadsafe making
        try (FileWriter stream = new FileWriter(path.toFile()); BufferedWriter out = new BufferedWriter(stream)) {
            for (String fromUri : table.keySet()) {
                String toUri = table.get(fromUri);
                out.write(fromUri + '\t' + toUri);
                out.newLine();
            }
        }
    }

    public void load(Path path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                // process the line.
                String[] fromTo = line.split("\t");
                if (fromTo.length > 1) {
                    table.put(fromTo[0], fromTo[1]);
                }
            }
        }
    }
}
