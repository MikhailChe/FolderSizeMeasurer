package ru.dolika.dsm;

import static java.awt.Font.MONOSPACED;
import static java.awt.Font.PLAIN;
import static javax.swing.JFileChooser.FILES_AND_DIRECTORIES;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

public class SizeMeaurerGUI {

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		final JFrame frame = new JFrame();
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.pack();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setVisible(true);

		final JFileChooser fc = new JFileChooser() {
			private static final long serialVersionUID = 2295215359313159290L;

			@Override
			public void approveSelection() {
				// if (this.getSelectedFile().exists())
				// super.approveSelection();
				this.fireActionPerformed("SizeSelection");
				File curdir = getSelectedFile();
				if (curdir != null && curdir.exists())
					this.setCurrentDirectory(curdir);
			}
		};

		fc.setFileSelectionMode(FILES_AND_DIRECTORIES);
		fc.setAccessory(new JComponent() {

			private static final long serialVersionUID = 577952875484509442L;
			final DefaultListModel<String> listModel = new DefaultListModel<>();
			final JList<String> sizeList = new JList<>(listModel);

			final JScrollPane scrollPane = new JScrollPane(sizeList);

			{
				// scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				scrollPane.setMinimumSize(new Dimension(600, 800));
				setLayout(new BorderLayout(8, 8));
				add(scrollPane);

				sizeList.setFont(new Font(MONOSPACED, PLAIN, 16));

				fc.addPropertyChangeListener("SelectedFileChangedProperty", pe -> {
					if (pe.getNewValue() == null) {
						listModel.clear();
					}
				});
				scrollPane.doLayout();

				fc.addActionListener(evt -> {
					if ("SizeSelection".equals(evt.getActionCommand())) {
						final File file = fc.getSelectedFile();
						System.out.println("Sizing file: " + file);
						if (file != null && file.exists()) {
							listModel.clear();

							try {
								String fname = file.getCanonicalPath().trim();
								listModel
										.addElement("Computing size in " + (fname.length() > 12 ? "..." : "")
												+ fname.substring(Math.max(0, fname.length() - 12), fname.length()));
							} catch (IOException e) {
								e.printStackTrace();
							}

							System.out.println("YOYOYO" + file.getName());
							new Thread(() -> {
								Arrays
										.asList(file.listFiles())
										.stream()
										.map(f -> new Node<>(f, calculateSize(f)))
										.sorted()
										.forEachOrdered(el -> SwingUtilities
												.invokeLater(() -> listModel
														.addElement(String
																.format("%-20s | %10s", el.key
																		.getName()
																		.substring(0, Math
																				.min(el.key.getName().length(), 20)),
																		sizeToString(el.value)))));
								SwingUtilities.invokeLater(() -> {
									scrollPane.invalidate();
									scrollPane.validate();
									scrollPane.getParent().invalidate();
									scrollPane.getParent().validate();
									fc.invalidate();
									fc.revalidate();
									fc.validate();
								});
							}).start();
						}
					}
				});
			}

		});

		fc.setDialogTitle("Размер папки");

		fc.showDialog(frame, "Размер");
		System.exit(0);

	}

	static Map<File, Long> knownSizes = new HashMap<>();

	public static String sizeToString(long size) {
		if (size > 1024L * 1024L * 1024L) {
			return String.format("%.2f Гб", size / 1024f / 1024f / 1024f);
		} else if (size > 1024L * 1024L) {
			return String.format("%.2f Мб", size / 1024f / 1024f);
		} else if (size > 1024) {
			return String.format("%.2f кб", size / 1024f);
		}
		return size + " б";

	}

	public static long calculateSize(final File f) {
		if (f == null)
			return 0;
		if (f.isDirectory()) {
			return knownSizes.computeIfAbsent(f, a -> {
				File[] files = a.listFiles();
				if (files == null)
					return 0L;
				return Arrays.asList(a.listFiles()).stream().parallel().mapToLong(SizeMeaurerGUI::calculateSize).sum();
			});

		}
		return f.length();

	}
}

class Node<K, V extends Comparable<V>> implements Comparable<Node<K, V>> {
	K key;
	V value;

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	public Node(K key, V value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node<?, ?> other = (Node<?, ?>) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public int compareTo(Node<K, V> o) {

		return ((Comparable<V>) value).compareTo(o.value);
	}

}