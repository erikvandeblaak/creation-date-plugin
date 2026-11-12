package nl.vandeblaak.creationdate;

import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shows the creation date balloon when the mouse rests on a file or directory in the Project view.
 */
@Service(Service.Level.APP)
public final class CreationDateHoverService implements AWTEventListener, Disposable {

    private static final int DELAY_MS = 400;
    private static final long EVENT_MASK =
            AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK;

    private final AtomicBoolean installed = new AtomicBoolean();
    private final Timer timer = new Timer(DELAY_MS, e -> showBalloon());

    // Hover state, only touched on the EDT
    private @Nullable JTree tree;
    private int row = -1;
    private @Nullable VirtualFile file;
    private Point mouse = new Point();
    private @Nullable Balloon balloon;

    public CreationDateHoverService() {
        timer.setRepeats(false);
    }

    void install() {
        if (installed.compareAndSet(false, true)) {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, EVENT_MASK);
        }
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if (!(event instanceof MouseEvent e)) {
            return;
        }
        switch (e.getID()) {
            case MouseEvent.MOUSE_MOVED -> onMouseMoved(e);
            case MouseEvent.MOUSE_EXITED -> {
                if (e.getComponent() == tree) {
                    reset();
                }
            }
            case MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_DRAGGED, MouseEvent.MOUSE_WHEEL -> reset();
            default -> {
            }
        }
    }

    private void onMouseMoved(MouseEvent e) {
        // Movement over other components (e.g. the balloon itself) leaves the current state alone
        if (!(e.getComponent() instanceof JTree t)) {
            return;
        }
        int r = rowAt(t, e.getPoint());
        VirtualFile f = r < 0 ? null : fileAt(t, r);
        if (f == null) {
            reset();
            return;
        }
        if (t != tree || r != row || !f.equals(file)) {
            reset();
            tree = t;
            row = r;
            file = f;
            timer.restart();
        }
        mouse = e.getPoint();
    }

    private void showBalloon() {
        JTree t = tree;
        Instant created = CreationDates.of(file);
        Rectangle bounds = t == null ? null : t.getRowBounds(row);
        if (created == null || bounds == null || !t.isShowing()) {
            return;
        }
        // Point at the end of the node text, but never under the cursor and never beyond the visible area
        Rectangle visible = t.getVisibleRect();
        int x = Math.max(bounds.x + bounds.width, mouse.x + JBUI.scale(12));
        x = Math.min(x, visible.x + visible.width - JBUI.scale(4));
        Point anchor = new Point(x, bounds.y + bounds.height / 2);

        balloon = CreationDateBalloon.create(created);
        balloon.show(new RelativePoint(t, anchor), Balloon.Position.atRight);
    }

    private void reset() {
        timer.stop();
        tree = null;
        row = -1;
        file = null;
        if (balloon != null) {
            balloon.hide();
            balloon = null;
        }
    }

    /**
     * @return the row whose full height contains the point, or -1
     */
    static int rowAt(JTree tree, Point p) {
        int r = tree.getClosestRowForLocation(p.x, p.y);
        Rectangle bounds = r < 0 ? null : tree.getRowBounds(r);
        return bounds != null && p.y >= bounds.y && p.y < bounds.y + bounds.height ? r : -1;
    }

    /**
     * @return the file or directory of a Project view node at the given row, or {@code null} for other nodes
     */
    static @Nullable VirtualFile fileAt(JTree tree, int row) {
        TreePath path = tree.getPathForRow(row);
        Object node = path == null ? null : TreeUtil.getUserObject(path.getLastPathComponent());
        if (!(node instanceof ProjectViewNode<?> viewNode)) {
            return null;
        }
        return ReadAction.computeBlocking(viewNode::getVirtualFile);
    }

    /**
     * @return the file whose balloon is pending or showing; for tests
     */
    @Nullable VirtualFile hoveredFile() {
        return file;
    }

    @Override
    public void dispose() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        reset();
    }
}
