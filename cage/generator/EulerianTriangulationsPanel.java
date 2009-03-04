package cage.generator;

import cage.EmbedFactory;
import cage.GeneratorInfo;
import cage.GeneratorPanel;
import cage.StaticGeneratorInfo;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.Vector;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import lisken.uitoolbox.EnhancedSlider;

public class EulerianTriangulationsPanel extends GeneratorPanel {

    //The minimum number of vertices allowed for this generator
    private static final int MIN_VERTICES = 6;
    //The maximum number of vertices allowed for this generator
    private static final int MAX_VERTICES = 40;
    //The default number of vertices for this generator
    private static final int DEFAULT_VERTICES = 6;

    //slider to select number of vertices
    private EnhancedSlider verticesSlider;
    //buttons to select the minimum connectivity
    private ButtonGroup minConnGroup;
    //checkbox to generate only graphs with exactly the minimum connectivity
    private JCheckBox exactConn;

    public EulerianTriangulationsPanel() {
        setLayout(new GridBagLayout());
        verticesSlider = new EnhancedSlider();
        verticesSlider.setMinimum(MIN_VERTICES);
        verticesSlider.setMaximum(MAX_VERTICES);
        verticesSlider.setValue(DEFAULT_VERTICES);
        verticesSlider.setMinorTickSpacing(1);
        verticesSlider.setMajorTickSpacing(MAX_VERTICES - MIN_VERTICES);
        verticesSlider.setPaintTicks(true);
        verticesSlider.setPaintLabels(true);
        verticesSlider.setSnapWhileDragging(1);
        verticesSlider.setClickScrollByBlock(false);
        verticesSlider.setSizeFactor(4);
        add(verticesSlider,
                new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 5, 15, 0), 0, 0));
        JLabel verticesLabel = new JLabel("number of vertices");
        verticesLabel.setLabelFor(verticesSlider.slider());
        verticesLabel.setDisplayedMnemonic(KeyEvent.VK_V);
        add(verticesLabel,
                new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 20, 10), 0, 0));
        JLabel minConnLabel = new JLabel("minimum connectivity");
        add(minConnLabel,
                new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 15, 10), 0, 0));
        minConnGroup = new ButtonGroup();
        JPanel minConnPanel = new JPanel();
        for (int i = 3; i <= 4; ++i) {
            String is = Integer.toString(i);
            JRadioButton connButton = new JRadioButton(is, i == 3);
            connButton.setActionCommand(is);
            minConnGroup.add(connButton);
            minConnPanel.add(connButton);
        }
        add(minConnPanel,
                new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 10, 0), 0, 0));
        exactConn = new JCheckBox("only graphs with connectivity exactly the given minimum");
        exactConn.setMnemonic(KeyEvent.VK_R);
        add(exactConn,
                new GridBagConstraints(1, 3, 1, 1, 1.0, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 5, 0, 0), 0, 0));
    }

    public void showing() {
    }

    public GeneratorInfo getGeneratorInfo() {
        Vector genCmd = new Vector();
        String filename = "";

        genCmd.addElement("plantri");
        genCmd.addElement("-b");
        filename += "tri_euler";
        String v = Integer.toString(verticesSlider.getValue());
        filename += "_" + v;
        String minConn = minConnGroup.getSelection().getActionCommand();
        if (minConn.charAt(0) < '4') {
            minConn += exactConn.isSelected() ? "x" : "";
        }
        genCmd.addElement("-c" + minConn);
        filename += "_c" + minConn;
        genCmd.addElement(v);

        String[][] generator = new String[1][genCmd.size()];
        genCmd.copyInto(generator[0]);

        String[][] embed2D = {{"embed"}};
        String[][] embed3D = {{"embed", "-d3", "-it"}};

        return new StaticGeneratorInfo(
                generator,
                EmbedFactory.createEmbedder(true, embed2D, embed3D),
                filename, 3, true);
    }
}
