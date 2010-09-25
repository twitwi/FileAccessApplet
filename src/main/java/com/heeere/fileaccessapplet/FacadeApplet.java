/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.heeere.fileaccessapplet;

import java.applet.Applet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author twilight
 */
public class FacadeApplet extends Applet {

    public interface Task<T> {
        T execute();
    }


    private static class TaskWithResult<T> {
        Task<T> task;
        T result;

        public TaskWithResult(Task<T> task) {
            this.task = task;
        }

    }

    BlockingQueue<TaskWithResult<?>> tasks = new ArrayBlockingQueue<TaskWithResult<?>>(100);

    @Override
    public void init() {
        new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        TaskWithResult task;
                        task = tasks.take();
                        try {
                            task.result = task.task.execute();
                        } catch (Throwable t) {
                            // catch everything to avoid infinite wait of the other thread
                        }
                        synchronized (task) {
                            task.notify();
                        }
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(FacadeApplet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }.start();
        /*        try {
            //System.err.println(readFile("/tmp/test-file-applet.html")+"\n== 1 ==");
            writeDom(readDom("/tmp/test-file-applet.html").getDocumentElement(), "/tmp/test-file-applet2.html");
        } catch (InterruptedException ex) {
            Logger.getLogger(FacadeApplet.class.getName()).log(Level.SEVERE, null, ex);
            }*/
    }


    private <T> T execute(Task<T> task) throws InterruptedException {
        TaskWithResult<T> taskWithResult = new TaskWithResult<T>(task);
        synchronized (taskWithResult) {
            tasks.put(taskWithResult);
            taskWithResult.wait();
        }
        return taskWithResult.result;
    }

    public String separator() {
        return File.separator;
    }

    public String[] listFiles(final String dirName) throws InterruptedException {
        return execute(new Task<String[]>() {
            public String[] execute() {
                ArrayList<String> res = new ArrayList<String>();
                for (File f : new File(dirName).listFiles()) {
                    try {
                        res.add(f.getCanonicalPath() + (f.isDirectory() ? File.separator : ""));
                    } catch (IOException ex) {
                        Logger.getLogger(FacadeApplet.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                Collections.sort(res);
                return res.toArray(new String[]{});
            }
        });
    }

    public String readFile(final String fileName) throws InterruptedException {
        return execute(new Task<String>() {
            public String execute() {
                return simpleReadFile(fileName);
            }
        });
    }

    public boolean createFile(final String content, final String fileName) throws InterruptedException {
        return execute(new Task<Boolean>() {
            public Boolean execute() {
                if (new File(fileName).exists()) return false;
                simpleWriteFile(content, fileName);
                return true;
            }
       });
    }

    public void writeFile(final String content, final String fileName) throws InterruptedException {
        execute(new Task<Void>() {
            public Void execute() {
                simpleWriteFile(content, fileName);
                return null;
            }
       });
    }

    private String simpleReadFile(String fileName) {
        String line, result = "";
        try {
            FileInputStream fin = new FileInputStream(fileName);
            BufferedReader in = new BufferedReader(new InputStreamReader(fin));
            while ((line = in.readLine()) != null) {
                result += line + "\n";
            }
            in.close();
        } catch (Exception e) {
            throw new RuntimeException("readFile failed", e);
        }
        return result;
    }

    private void simpleWriteFile(String content, String fileName) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
            out.write(content);
            out.close();
        } catch (Exception e) {
            throw new RuntimeException("writeFile failed", e);
        }
    }
    
    // signing
    // http://ezzatron.com/2009/09/29/automatically-signing-jar-files-in-netbeans/

    public Document readDom(final String filePath) throws InterruptedException {
        return execute(new Task<Document>() {
            public Document execute() {
                try {
                    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(filePath));
                } catch (Exception ex) {
                    throw new RuntimeException("readDom failed", ex);
                }
            }
        });
    }

    public void writeDom(final Element root, final String filePath) throws InterruptedException {
        execute(new Task<Void>() {
            public Void execute() {
                try {
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    transformerFactory.setAttribute("indent-number", new Integer(2));
                    Transformer trans = transformerFactory.newTransformer();
                    trans.setOutputProperty(OutputKeys.INDENT, "yes");
                    trans.transform(new DOMSource(root), new StreamResult(new File(filePath)));
                } catch (Exception ex) {
                    throw new RuntimeException("writeDom failed", ex);
                }
                return null;
            }
        });
    }

    XPathFactory xpf = XPathFactory.newInstance();
    XPath xpath  = xpf.newXPath();

    public String xpath(final Element base, final String path) throws InterruptedException {
        return execute(new Task<String>() {
            public String execute() {
                try {
                    return xpath.evaluate(path, base);
                } catch (XPathExpressionException ex) {
                    Logger.getLogger(FacadeApplet.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            }
        });
    }

}
