package nl.vandeblaak.creationdate;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

public class CreationDateTest extends BasePlatformTestCase {

    private static final Instant CREATED = Instant.parse("2020-03-15T10:20:00Z");
    private static final ZoneId AMSTERDAM = ZoneId.of("Europe/Amsterdam");

    @Override
    protected TempDirTestFixture createTempDirTestFixture() {
        // Real files on disk instead of the in-memory temp:// file system
        return new TempDirTestFixtureImpl();
    }

    public void testPluginIsLoadedAndStartupInstallsService() throws Exception {
        IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("nl.vandeblaak.creationdate"));
        assertNotNull("plugin not loaded", plugin);
        try (var xml = getClass().getResourceAsStream("/META-INF/plugin.xml")) {
            assertNotNull(xml);
            String text = new String(xml.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(text.contains("<postStartupActivity implementation=\"" + CreationDateStartup.class.getName() + "\""));
        }

        assertEquals(Unit.INSTANCE, new CreationDateStartup().execute(getProject(), new EmptyContinuation()));
        assertNotNull(ApplicationManager.getApplication().getService(CreationDateHoverService.class));
    }

    public void testReadsCreationDateOfFile() throws Exception {
        assertReadsCreationDate(myFixture.addFileToProject("src/index.txt", "hello").getVirtualFile());
    }

    public void testReadsCreationDateOfDirectory() throws Exception {
        assertReadsCreationDate(myFixture.addFileToProject("dir/sub/a.txt", "").getVirtualFile().getParent());
    }

    private static void assertReadsCreationDate(VirtualFile file) throws Exception {
        Path path = file.toNioPath();
        Files.setAttribute(path, "basic:creationTime", FileTime.from(CREATED));
        // Windows and macOS can set the creation time; Linux keeps the real one, so compare with what the disk reports
        if (SystemInfo.isWindows || SystemInfo.isMac) {
            assertEquals(CREATED, CreationDates.of(file));
        } else {
            Instant onDisk = Files.readAttributes(path, BasicFileAttributes.class).creationTime().toInstant();
            assertEquals(onDisk, CreationDates.of(file));
        }
    }

    public void testNonLocalFileHasNoDate() throws Exception {
        LightTempDirTestFixtureImpl memoryFs = new LightTempDirTestFixtureImpl(true);
        memoryFs.setUp();
        try {
            assertNull(CreationDates.of(memoryFs.createFile("mem.txt", "x")));
            assertNull(CreationDates.of(null));
        } finally {
            memoryFs.tearDown();
        }
    }

    public void testDeletedFileHasNoDate() throws Exception {
        VirtualFile file = myFixture.addFileToProject("gone.txt", "").getVirtualFile();
        // Remove from disk behind the VFS' back: the VirtualFile stays valid but reading attributes fails
        Files.delete(file.toNioPath());

        assertNull(CreationDates.of(file));
    }

    public void testDutchFormatting() {
        assertEquals("Zondag 15 maart 2020", CreationDateBalloon.formatDate(CREATED, AMSTERDAM, CreationDateMessages.DUTCH));
        assertEquals("om 11:20", CreationDateBalloon.formatTime(CREATED, AMSTERDAM, CreationDateMessages.DUTCH));
        assertEquals("Aangemaakt", CreationDateMessages.get("balloon.header", CreationDateMessages.DUTCH));
    }

    public void testEnglishFormattingAlsoOnDutchSystem() {
        Locale before = Locale.getDefault();
        Locale.setDefault(CreationDateMessages.DUTCH);
        try {
            assertEquals("Sunday, March 15, 2020", CreationDateBalloon.formatDate(CREATED, AMSTERDAM, Locale.ENGLISH));
            assertEquals("at 11:20", CreationDateBalloon.formatTime(CREATED, AMSTERDAM, Locale.ENGLISH));
            assertEquals("Created", CreationDateMessages.get("balloon.header", Locale.ENGLISH));
        } finally {
            Locale.setDefault(before);
        }
    }

    public void testLanguageFollowsSystem() {
        Locale before = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("nl-BE"));
            assertEquals(CreationDateMessages.DUTCH, CreationDateMessages.locale());
            Locale.setDefault(Locale.GERMANY);
            assertEquals(Locale.ENGLISH, CreationDateMessages.locale());
        } finally {
            Locale.setDefault(before);
        }
    }

    public void testFindsFileAndDirectoryUnderMouse() {
        PsiFile psiFile = myFixture.addFileToProject("pkg/file.txt", "");
        JTree tree = projectTree(psiFile);

        assertEquals(psiFile.getVirtualFile().getParent(), CreationDateHoverService.fileAt(tree, 0));
        assertEquals(psiFile.getVirtualFile(), CreationDateHoverService.fileAt(tree, 1));

        Rectangle fileRow = tree.getRowBounds(1);
        // Anywhere on the row counts, also right of the file name
        assertEquals(1, CreationDateHoverService.rowAt(tree, new Point(fileRow.x + fileRow.width + 50, fileRow.y + 1)));
        assertEquals(-1, CreationDateHoverService.rowAt(tree, new Point(5, fileRow.y + fileRow.height + 40)));
    }

    public void testIgnoresNodesWithoutFile() {
        JTree tree = new JTree(new DefaultMutableTreeNode("External Libraries"));
        assertNull(CreationDateHoverService.fileAt(tree, 0));
    }

    public void testHoverStateFollowsMouse() {
        PsiFile psiFile = myFixture.addFileToProject("pkg/file.txt", "");
        JTree tree = projectTree(psiFile);
        Rectangle fileRow = tree.getRowBounds(1);
        CreationDateHoverService service = new CreationDateHoverService();
        try {
            service.eventDispatched(mouse(tree, MouseEvent.MOUSE_MOVED, fileRow.x + 2, fileRow.y + 2));
            assertEquals(psiFile.getVirtualFile(), service.hoveredFile());

            // Movement over another component (like the balloon) keeps the state
            service.eventDispatched(mouse(new JPanel(), MouseEvent.MOUSE_MOVED, 1, 1));
            assertEquals(psiFile.getVirtualFile(), service.hoveredFile());

            service.eventDispatched(mouse(tree, MouseEvent.MOUSE_MOVED, 2, fileRow.y + fileRow.height + 40));
            assertNull(service.hoveredFile());

            service.eventDispatched(mouse(tree, MouseEvent.MOUSE_MOVED, fileRow.x + 2, fileRow.y + 2));
            service.eventDispatched(mouse(tree, MouseEvent.MOUSE_PRESSED, fileRow.x + 2, fileRow.y + 2));
            assertNull(service.hoveredFile());

            service.eventDispatched(mouse(tree, MouseEvent.MOUSE_MOVED, fileRow.x + 2, fileRow.y + 2));
            service.eventDispatched(mouse(tree, MouseEvent.MOUSE_EXITED, -1, -1));
            assertNull(service.hoveredFile());
        } finally {
            service.dispose();
        }
    }

    /**
     * Renders the balloon content like the balloon does (yellow fill, rounded border) to build/preview.
     */
    public void testRenderPreview() throws Exception {
        JComponent content = CreationDateBalloon.createContent(CREATED, AMSTERDAM, CreationDateMessages.DUTCH);
        Dimension size = content.getPreferredSize();
        content.setSize(size);
        layoutRecursively(content);

        int pad = 12;
        int scale = 2;
        int w = size.width + 2 * pad;
        int h = size.height + 2 * pad;
        BufferedImage image = new BufferedImage((w + 20) * scale, (h + 20) * scale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.scale(scale, scale);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x2B2D30));
        g.fillRect(0, 0, w + 20, h + 20);
        RoundRectangle2D shape = new RoundRectangle2D.Double(10, 10, w, h,
                2 * CreationDateBalloon.CORNER_RADIUS, 2 * CreationDateBalloon.CORNER_RADIUS);
        g.setColor(CreationDateBalloon.FILL);
        g.fill(shape);
        g.setColor(CreationDateBalloon.BORDER);
        g.draw(shape);
        g.translate(10 + pad, 10 + pad);
        content.paint(g);
        g.dispose();

        File out = new File("build/preview/balloon.png");
        assertTrue(out.getParentFile().isDirectory() || out.getParentFile().mkdirs());
        ImageIO.write(image, "png", out);
        assertTrue(size.width > 100 && size.height > 30);
    }

    private JTree projectTree(PsiFile psiFile) {
        DefaultMutableTreeNode dirNode = new DefaultMutableTreeNode(
                new PsiDirectoryNode(getProject(), psiFile.getContainingDirectory(), ViewSettings.DEFAULT));
        dirNode.add(new DefaultMutableTreeNode(new PsiFileNode(getProject(), psiFile, ViewSettings.DEFAULT)));
        JTree tree = new JTree(dirNode);
        tree.setSize(300, 200);
        tree.expandRow(0);
        return tree;
    }

    private static final class EmptyContinuation implements Continuation<Unit> {
        @Override
        public @NotNull CoroutineContext getContext() {
            return EmptyCoroutineContext.INSTANCE;
        }

        @Override
        public void resumeWith(@NotNull Object result) {
        }
    }

    private static MouseEvent mouse(Component source, int id, int x, int y) {
        return new MouseEvent(source, id, System.currentTimeMillis(), 0, x, y, 0, false);
    }

    private static void layoutRecursively(Component c) {
        c.doLayout();
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                layoutRecursively(child);
            }
        }
    }
}
