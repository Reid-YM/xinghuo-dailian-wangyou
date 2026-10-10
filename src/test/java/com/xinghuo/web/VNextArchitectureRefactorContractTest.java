package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class VNextArchitectureRefactorContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void reactEntryOnlyBootstrapsTheApplication() throws IOException {
        String entry = read("frontend-vnext/src/main.tsx");

        assertTrue(entry.contains("import { App } from './app/App'"));
        assertTrue(entry.contains("createRoot(document.getElementById('root')!)"));
        assertFalse(entry.contains("function Home"));
        assertFalse(entry.contains("function Quote"));
        assertFalse(entry.contains("loadPublicBootstrap"));
        assertTrue(entry.length() < 700, "main.tsx must remain a bootstrap-only entrypoint");
    }

    @Test
    void applicationShellAndPagesHaveExplicitModuleBoundaries() throws IOException {
        String app = read("frontend-vnext/src/app/App.tsx");
        String navigation = read("frontend-vnext/src/app/navigation.tsx");

        assertTrue(app.contains("HomePage"));
        assertTrue(app.contains("ServicesPage"));
        assertTrue(app.contains("QuotePage"));
        assertTrue(app.contains("RecordsPage"));
        assertTrue(app.contains("AccountPage"));
        assertTrue(app.contains("loadPublicBootstrap"));
        assertTrue(navigation.contains("export type PageKey"));
        assertTrue(navigation.contains("export function pageFromPath"));

        for (String page : new String[] {"HomePage", "ServicesPage", "QuotePage", "RecordsPage", "AccountPage"}) {
            assertTrue(Files.exists(ROOT.resolve("frontend-vnext/src/pages/" + page + ".tsx")), page + " must stay in pages/");
        }
        assertTrue(Files.exists(ROOT.resolve("frontend-vnext/src/components/public-content.tsx")));
    }
}
