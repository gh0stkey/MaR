package mar.utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

public class WrapLayout extends FlowLayout {

    public WrapLayout() {
        super();
    }

    public WrapLayout(int align) {
        super(align);
    }

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return layoutSize(target, false);
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int maxWidth = target.getWidth() - insets.left - insets.right - getHgap() * 2;

            if (maxWidth <= 0) {
                return preferred ? super.preferredLayoutSize(target) : super.minimumLayoutSize(target);
            }

            int rowWidth = 0;
            int rowHeight = 0;
            int totalHeight = 0;
            int componentCount = target.getComponentCount();

            for (int i = 0; i < componentCount; i++) {
                Component component = target.getComponent(i);
                if (!component.isVisible()) {
                    continue;
                }

                Dimension dimension = preferred ? component.getPreferredSize() : component.getMinimumSize();

                if (rowWidth + dimension.width > maxWidth && rowWidth > 0) {
                    totalHeight += rowHeight + getVgap();
                    rowWidth = 0;
                    rowHeight = 0;
                }

                if (rowWidth > 0) {
                    rowWidth += getHgap();
                }
                rowWidth += dimension.width;
                rowHeight = Math.max(rowHeight, dimension.height);
            }

            totalHeight += rowHeight;
            totalHeight += insets.top + insets.bottom + getVgap() * 2;

            int width = Math.max(maxWidth, 0) + insets.left + insets.right + getHgap() * 2;
            return new Dimension(width, totalHeight);
        }
    }
}