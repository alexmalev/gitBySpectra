import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDVarSet;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.util.FS;
import tau.smlab.syntech.games.controller.symbolic.SymbolicController;
import tau.smlab.syntech.games.controller.symbolic.SymbolicControllerReaderWriter;
import tau.smlab.syntech.jtlv.BDDPackage;
import tau.smlab.syntech.jtlv.Env;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by alexanderm on 23/02/2018.
 */
public class GitGui extends JFrame {
    private BDD currentState;
    private SymbolicController ctrl;
    private boolean initialState = true;
    private File repoDirectory;
    private File fileToAdd;
    private JLabel directoryLabel;
    private JLabel filesLabel;
    private JButton cloneButton;
    private JButton openButton;
    private JButton addButton;
    private JButton commitButton;
    private JButton filesButton;
    private GitGui gitGui = this;
    private GitController controller = new GitController();

    public GitGui() {
        this.setSize(800, 600);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Spectra Git");
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        JPanel directoryPanel = new JPanel();
        directoryPanel.setLayout(new FlowLayout());
        JPanel repoPanel = new JPanel();
        repoPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JPanel filesPanel = new JPanel();
        filesPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JPanel commitPanel = new JPanel();
        commitPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        directoryLabel = new JLabel("(none selected)");
        cloneButton = new JButton("clone");
        cloneButton.setEnabled(false);
        cloneButton.addActionListener(e -> {
            try {
                controller.cloneRepo(repoDirectory);

            } catch (GitAPIException e1) {
                e1.printStackTrace();
            }
            updateState();
        });
        openButton = new JButton("open");
        openButton.setEnabled(false);
        openButton.addActionListener(e -> {
            controller.openExistingRepo(repoDirectory);
            updateState();
        });
        filesButton = new JButton("file to add");
        filesButton.setEnabled(false);
        filesLabel = new JLabel("(none selected)");
        filesButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(repoDirectory.getAbsolutePath()));
            chooser.setDialogTitle("choose file to add");
            if (chooser.showOpenDialog(gitGui) == JFileChooser.APPROVE_OPTION) {
                fileToAdd = chooser.getSelectedFile();
                filesLabel.setText(fileToAdd.getName());
                controller.setFileStatus(fileToAdd.getName());
            }
            updateState();
        });
        addButton = new JButton("add");
        addButton.setEnabled(false);
        addButton.addActionListener(e -> {
            controller.add(fileToAdd);
            controller.setFileStatus(fileToAdd.getName());
            updateState();
        });
        commitButton = new JButton("commit");
        commitButton.setEnabled(false);
        cloneButton.addActionListener(e -> {
            controller.setFileStatus(fileToAdd.getName());
            updateState();
        });
        JButton directoryButton = new JButton("choose a directory");
        directoryButton.setToolTipText("empty directory to clone to, or existing repository");
        directoryButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            chooser.setDialogTitle("choose a directory");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(gitGui) == JFileChooser.APPROVE_OPTION) {
                controller.setRepository(null);
                repoDirectory = chooser.getSelectedFile();
                directoryLabel.setText(repoDirectory.getAbsolutePath());
                updateRepoStatus();
            }
            updateState();
        });
        directoryPanel.add(directoryButton);
        directoryPanel.add(directoryLabel);
        repoPanel.add(cloneButton);
        repoPanel.add(openButton);
        filesPanel.add(filesButton);
        filesPanel.add(filesLabel);
        commitPanel.add(addButton);
        commitPanel.add(commitButton);
        mainPanel.add(directoryPanel);
        mainPanel.add(repoPanel);
        mainPanel.add(filesPanel);
        mainPanel.add(commitPanel);
        this.add(mainPanel, BorderLayout.WEST);
        this.setVisible(true);
    }

    private void updateRepoStatus() {
        File gitDirectory = new File(repoDirectory.getAbsolutePath() + "\\.git");
        boolean isRepo = RepositoryCache.FileKey.isGitRepository(gitDirectory, FS.DETECTED);
        if (isRepo) {
            controller.setRepoStatus(RepoStatus.Valid);
            return;
        }
        if (repoDirectory.list().length > 0) {
            controller.setRepoStatus(RepoStatus.Invalid);
            return;
        }
        controller.setRepoStatus(RepoStatus.Empty);
    }

    public static void main(String[] args) {
        GitGui gitGui = new GitGui();
        gitGui.run();

    }

    private void loadController() {
        BDDPackage.setCurrPackage(BDDPackage.JTLV);

        try {
            ctrl = SymbolicControllerReaderWriter.readSymbolicController("out/");
        } catch (Exception e) {
            e.printStackTrace();
        }
        currentState = ctrl.initial().id();
        initialState = true;
    }

    private void run() {
        loadController();
        updateState();
    }

    private void updateState() {
        // compute next BDD
        if (initialState) {
            BDD one = currentState.satOne(Env.globalUnprimeVars());
            currentState.free();
            currentState = one;
            initialState = false;
        } else {
            BDD succs = ctrl.succ(currentState);
            BDD succsWithVehicles = setEnvState(succs); //TODO
            succs.free();
            java.util.List<BDD> choices = new ArrayList<>();
            BDD.BDDIterator it = new BDD.BDDIterator(succsWithVehicles, Env.globalUnprimeVars());
            while (it.hasNext()) {
                choices.add(it.next());
            }
            int pick = (int) Math.floor(Math.random() * choices.size());
            currentState = choices.get(pick).id();
            Env.free(choices);
            succsWithVehicles.free();
        }
        System.out.println(Env.toNiceSignleLineString(currentState));

        // set values of class variables according to BDD
        String state = currentState.toStringWithDomains(Env.stringer);
        String[] stateVals = state.replace("<", "").replace(">", "").replace(" ", "").split(",");
        boolean canOpen = false;
        boolean canClone = false;
        boolean canAdd = false;
        boolean canCommit = false;
        for (String s : stateVals) {
            String[] val = s.split(":");
            if ("open".equals(val[0])) {
                canOpen = Boolean.valueOf(val[1]);
            } else if ("clone".equals(val[0])) {
                canClone = Boolean.valueOf(val[1]);
            } else if ("add".equals(val[0])) {
                canAdd = Boolean.valueOf(val[1]);
            } else if ("commit".equals(val[0])) {
                canCommit = Boolean.valueOf(val[1]);
            }
        }
        cloneButton.setEnabled(canClone);
        openButton.setEnabled(canOpen);
        filesButton.setEnabled(true);
        addButton.setEnabled(canAdd);
        commitButton.setEnabled(true);


    }

    private BDD setEnvState(BDD succs) {
        BDDVarSet vehiclesVars = Env.getVar("repoStatus").support()
                .union(Env.getVar("fileStatus").support());
        BDD.BDDIterator it = new BDD.BDDIterator(succs, vehiclesVars);
        Set<String> envStates = new HashSet<>();
        while (it.hasNext()) {
            BDD envState = it.next();
            String state = envState.toStringWithDomains(Env.stringer);
            if (!envStates.contains(state)) {
                envStates.add(state);
            }
        }

        if (envStates.size() > 1) {
            String repoStatus = controller.getRepoStatus().toString();
            String fileStatus = controller.getFileStatus().toString();
            BDD succWithCurrentState = succs.and(Env.getBDDValue("repoStatus", repoStatus))
                    .and(Env.getBDDValue("fileStatus", fileStatus));
            return succWithCurrentState;
        } else {
            return succs.id();
        }
    }
}
