package es.ucm.fdi.ici.fsm.observers;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.swing_viewer.DefaultView;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.view.Viewer;

import es.ucm.fdi.ici.fsm.FSMObserver;

/**
 * Graphical observer to evaluate FSM using the GraphStream library.
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 */
public class GraphFSMObserver implements FSMObserver {

	private DefaultView view;
	
	Graph graph;
	public GraphFSMObserver(String fsmId) {
		view = null;
		System.setProperty("org.graphstream.ui", "swing");
		graph = new MultiGraph(fsmId);
	}

	@Override
	public void fsmAdd(String sourceState, String transition, String targetState) {
		
		if(graph.getNode(sourceState)==null)
			graph.addNode(sourceState);
		if(graph.getNode(targetState)==null)
			graph.addNode(targetState);
		graph.addEdge(transition, sourceState, targetState, true);

	}

	@Override
	public void fsmTransition(String sourceState, String transition, String targetState) {
		Node n = graph.getNode(targetState);
		n.setAttribute("ui.color", 1.0f);

		for(int i=0; i<n.getOutDegree(); i++) {
			n.getEdge(i).setAttribute("label", "");
			n.getEdge(i).setAttribute("ui.color", 0.0f);
		}
		
		n = graph.getNode(sourceState);
		n.setAttribute("ui.color", 0.0f);

		for(int i=0; i<n.getOutDegree(); i++) {
			n.getEdge(i).setAttribute("label", "");
			n.getEdge(i).setAttribute("ui.color", 0.0f);
		}

		Edge edge = graph.getEdge(transition);
		edge.setAttribute("ui.color", 1.0f);
		edge.setAttribute("label", edge.getId());
	}

	@Override
	public void fsmReady(String initialState) {
		for(Node n:graph)
			n.setAttribute("label", n.getId());

		graph.setAttribute("ui.stylesheet",  "edge {z-index: -100; text-background-mode: rounded-box; text-background-color: white; text-padding: 2px; text-color: blue; text-offset: 0px, 10px; size: 4; text-alignment: along; shape: cubic-curve;arrow-shape: arrow; arrow-size: 12px, 8px;fill-mode: dyn-plain; fill-color: #ddd, #82b1ff;} node { size-mode: fit; shape: circle; fill-mode: dyn-plain; fill-color: white, #b6e3ff; stroke-mode: plain; stroke-color: #ddd; stroke-width: 4px; padding: 30px, 12px; text-offset: 0px, 8px; }");		
	}
	
	@Override
	public void fsmReset(String initialState) {
		for(Node n:graph)
			n.setAttribute("ui.color", 0.0f);
		
		Node n = graph.getNode(initialState);
		n.setAttribute("ui.color", 1.0f);
	}
	
	protected void createView() {
		if(view == null)
		{
			Viewer viewer = new SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
			viewer.enableAutoLayout();
			view = (DefaultView) viewer.addDefaultView(false);
		}		
	}
	
	/**
	 * Returns the graph as a panel to be included in a frame. 
	 * @param includeTitledBorder defines if a titled border is included around the panel.
	 * @param preferredSize  is an optional argument with the preferred size. If null, the method estimates a dimension.
	 * @return
	 */
	public JPanel getAsPanel(boolean includeTitledBorder, Dimension preferredSize)
	{
		createView();
		
		Dimension dim;
		if(preferredSize == null)
		{
			int extra = graph.getNodeCount()*50;
			dim = new Dimension(400+extra, 200+extra);
		}
		else 
			dim = preferredSize;
		
		view.setPreferredSize(dim);

		JPanel panel = new JPanel();
		panel.add(view);
		if(includeTitledBorder)
			view.setBorder(BorderFactory.createTitledBorder(graph.getId()));
		
		return view;
	}
	
	/**
	 * Shows a frame with the graph.
	 * @param preferredSize is an optional argument with the preferred size. If null, the method estimates a dimension.
	 */
	public void showInFrame(Dimension preferredSize)
	{
		createView();
		JFrame frame = new JFrame(graph.getId());
		frame.getContentPane().add(view);
		
		Dimension dim;
		if(preferredSize == null)
		{
			int extra = graph.getNodeCount()*50;
			dim = new Dimension(400+extra, 200+extra);
		}
		else 
			dim = preferredSize;		
		
		frame.setSize(dim);
		
		frame.setVisible(true);
	}


}
