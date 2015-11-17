package ru.ifmo.ctddev.shatrov.uifilecopy;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class UIFileCopy extends JFrame implements ActionListener {
    private JLabel timeElapsed;
    private JLabel timeRemaining;
    private JLabel averageSpeed;
    private JLabel currentSpeed;
    private JProgressBar progressBar;
    private JButton cancelButton;
    private Worker worker;

    /**
     * Creates and initializes application UI and starts copying.
     *
     * @param src path to file for copy
     * @param dst path to destination file
     */
    public UIFileCopy(String src, String dst) {
        super("UIFileCopy");
        File source = new File(src);
        File target = new File(dst);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        timeElapsed = new JLabel("Time past: 0");
        timeRemaining = new JLabel("Time remaining: 0:0");
        averageSpeed = new JLabel("Average speed: 0 Mb/s");
        currentSpeed = new JLabel("Current speed: 0 MB/s");
        Dimension labelDim = new Dimension(165, 23);
        currentSpeed.setMinimumSize(labelDim);
        averageSpeed.setMinimumSize(labelDim);
        timeElapsed.setMinimumSize(labelDim);
        timeRemaining.setMinimumSize(labelDim);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);


        JPanel panel = new JPanel();
        getContentPane().add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridBagLayout());


        JPanel infoPanel = new JPanel();
        bottomPanel.add(infoPanel);
        JPanel contPanel = new JPanel(new FlowLayout());
        cancelButton = new JButton("cancel");
        cancelButton.setActionCommand("cancel");
        cancelButton.addActionListener(this);
        contPanel.add(cancelButton);
        bottomPanel.add(contPanel);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        panel.add(progressBar);
        panel.add(bottomPanel);

        infoPanel.add(timeElapsed);
        infoPanel.add(timeRemaining);
        infoPanel.add(averageSpeed);
        infoPanel.add(currentSpeed);

        setMinimumSize(rootPane.getMinimumSize());
        setLocation(((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth() - getWidth()) / 2, ((int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() - getHeight()) / 2);
        pack();
        setVisible(true);

        if (!source.exists()) {
            JOptionPane.showConfirmDialog(null, "Source File not found", "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null);
            dispose();
        } else {
            worker = new Worker(source, target);
            worker.execute();
        }
    }


    /**
     * Listen to button pressing
     *
     * @param e represents which button was pressed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("cancel")) {
            worker.cancel(true);
            dispose();
        }
    }

    private class Info {
        int timeElapsed;
        int timeRemain;
        double currentSpeed;
        double averageSpeed;
        long copied;

        long sTime;
    }

    private class Worker extends SwingWorker<Void, Info> {
        private File source;
        private File target;
        private Info lastInfo;
        private long startTime;
        private long wholeSize;
        private long currentSize;
        private boolean rewriteAll;

        Worker(File source, File target) {
            this.source = source;
            this.target = target;
        }

        @Override
        protected Void doInBackground() throws Exception {
            rewriteAll = false;
            startTime = System.currentTimeMillis();
            wholeSize = 0;
            currentSize = 0;
            calcSize(source);
            lastInfo = new Info();
            lastInfo.sTime = startTime;
            lastInfo.copied = 0;
            copyWalk(source, target);
            publish(produceInfo());

            return null;
        }

        private void calcSize(File file) {
            if (file.isFile()) {
                wholeSize += file.length();
            } else if (file.isDirectory()) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(file.getPath()))) {
                    for (Path path : stream) {
                        calcSize(path.toFile());
                    }
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }

        private void copyWalk(File src, final File dst) {
            if (dst.exists() && !rewriteAll) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            String[] options = {"CANCEL", "REWRITE", "REWRITE ALL"};
                            int value = JOptionPane.showOptionDialog(null, "File " + dst.getPath() + " already exists", "Warning",
                                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                                    null, options, options[0]);
                            if (value == 0) {
                                worker.cancel(true);
                                dispose();
                            } else if (value == 2) {
                                rewriteAll = true;
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            if (src.isFile()) {
                try (InputStream is = new FileInputStream(src)) {
                    try (OutputStream os = new FileOutputStream(dst)) {
                        byte[] buffer = new byte[4096];
                        int length;
                        while ((length = is.read(buffer)) > 0) {
                            if (isCancelled()) {
                                dst.delete();
                                break;
                            }
                            os.write(buffer, 0, length);
                            currentSize += length;
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setValue((int) (currentSize * 100 / wholeSize));
                                }
                            });
                            if (System.currentTimeMillis() - lastInfo.sTime > 1000) {
                                System.err.println(System.currentTimeMillis() - lastInfo.sTime);
                                publish(produceInfo());
                            }
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (src.isDirectory()) {
                dst.mkdirs();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(src.getPath()))) {
                    for (Path path : stream) {
                        if (isCancelled()) {
                            break;
                        }
                        copyWalk(path.toFile(), new File(dst, path.getFileName().toString()));
                    }
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }

        private Info produceInfo() {
            Info info = new Info();
            info.copied = currentSize;
            info.sTime = System.currentTimeMillis();
            info.timeElapsed = (int) ((info.sTime - startTime) / 1000);
            info.currentSpeed = (info.copied - lastInfo.copied) / (1024 * 1024);
            info.averageSpeed = (info.copied / (1024 * 1024 * info.timeElapsed));
            info.timeRemain = (int) ((wholeSize - info.copied) / (1024 * 1024 * info.averageSpeed));

            lastInfo = info;
            return info;
        }


        @Override
        protected void done() {
            super.done();
            progressBar.setValue(100);
            cancelButton.setText("done");
        }

        @Override
        protected void process(List<Info> chunks) {
            Info current = chunks.get(chunks.size() - 1);
            timeElapsed.setText("Time past: " + (current.timeElapsed / 60) + ":" + (current.timeElapsed % 60));
            timeRemaining.setText("Time remaining: " + (current.timeRemain / 60) + ":" + (current.timeRemain % 60));
            averageSpeed.setText("Average speed: " + current.averageSpeed + " MB/s");
            currentSpeed.setText("Current speed: " + current.currentSpeed + " MB/s");
        }
    }

    /**
     * Start copying
     *
     * @param args path to source and target directory
     */

    public static void main(final String[] args) {
        if (args == null || args.length < 2 || args[0] == null || args[1] == null) {
            System.err.println("Wrong args. Usage: UIFileCopy source_dir target_dir");
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new UIFileCopy(args[0], args[1]);
            }
        });
    }

}
