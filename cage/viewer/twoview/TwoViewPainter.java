package cage.viewer.twoview;

import cage.EdgeIterator;
import cage.EmbeddableGraph;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class TwoViewPainter {

    TwoViewDevice device;
    EmbeddableGraph graph;
    int graphSize;
    float coordinate[][];
    FloatingPoint p[];
    double xMin, xMax, yMin, yMax;
    double horMin, horMax, verMin, verMax;
    int horSign, verSign;
    double scale, delta, horOffset, verOffset;

    boolean isPartOfPentagon[][];
    boolean highlightPentagons = false;
    boolean pentagonsAlreadyDetermined = false;

    public TwoViewPainter(TwoViewDevice device) {
        this.device = device;
    }

    public void setGraph(EmbeddableGraph graph) {
        this.graph = graph;
        coordinate = graph.get2DCoordinates();
        graphSize = graph.getSize();
        if (graphSize <= 0) {
            return;
        }
        xMin = xMax = coordinate[0][0];
        yMin = yMax = coordinate[0][1];
        for (int i = 0; i < graphSize; ++i) {
            xMin = Math.min(xMin, coordinate[i][0]);
            xMax = Math.max(xMax, coordinate[i][0]);
            yMin = Math.min(yMin, coordinate[i][1]);
            yMax = Math.max(yMax, coordinate[i][1]);
        }
        viewportChanged();

        isPartOfPentagon = new boolean[graphSize+1][graphSize+1];
        pentagonsAlreadyDetermined = false;
    }

    public void setHighlightPentagons(boolean highlightPentagons) {
        this.highlightPentagons = highlightPentagons;
    }

    /**
     * @param embedding a list with the ordered list of neighbours of each vertex
     * @return true if edge v1 v2 is part of a pentagon
     */
    private boolean edgeIsPartOfPentagon(int v1, int v2, List<List<Integer>> embedding) {
        int v1_temp = v1;
        int v2_temp = v2;

        //First investigating the face at one of the edge
        for(int i = 0; i < 5; i++) {
            List<Integer> neighbours = embedding.get(v2_temp);
            int previous_index = neighbours.indexOf(v1_temp);
            if(previous_index == -1) {
                throw new RuntimeException("Vertex " + v1_temp + " not found in list of neighbours of " + v2_temp);
            }
            v1_temp = v2_temp;
            v2_temp = neighbours.get((previous_index - 1 + neighbours.size()) % neighbours.size());
        }
        if(v1_temp == v1 && v2_temp == v2) {
            return true;
        }

        //Investigating the face to the other side of the edge
        for(int i = 0; i < 5; i++) {
            List<Integer> neighbours = embedding.get(v2_temp);
            int previous_index = neighbours.indexOf(v1_temp);
            if(previous_index == -1) {
                throw new RuntimeException("Vertex " + v1_temp + " not found in list of neighbours of " + v2_temp);
            }
            v1_temp = v2_temp;
            v2_temp = neighbours.get((previous_index + 1) % neighbours.size());
        }

        return v1_temp == v1 && v2_temp == v2;
    }

    private void determinePentagons() {
        //The neighbours of each vertex
        List<List<Integer>> embedding = new ArrayList<List<Integer>>();

        //For i = 0
        embedding.add(new ArrayList<Integer>());
        for (int i = 1; i <= graphSize; i++) {
            EdgeIterator it = graph.getEdgeIterator(i);
            List<Integer> neighbours = new ArrayList<Integer>();
            while(it.hasNext()) {
                neighbours.add(it.nextEdge());
            }
            embedding.add(neighbours);
        }

        for (int i = graphSize; i > 0; --i) {
            EdgeIterator it = graph.getEdgeIterator(i);
            while(it.hasNext()) {
                int j = it.nextEdge();

                //Algorithm is not really efficient, but it is certainly not a bottleneck
                if(edgeIsPartOfPentagon(i, j, embedding)) {
                    isPartOfPentagon[i][j] = true;
                    isPartOfPentagon[j][i] = true;
                }
            }
        }

        pentagonsAlreadyDetermined = true;

    }

    public void setPaintArea(double horMin, double horMax, double verMin, double verMax) {
        this.verMin = verMin;
        this.verMax = verMax;
        if (horMin <= horMax) {
            horSign = +1;
            this.horMin = horMin;
            this.horMax = horMax;
        } else {
            horSign = -1;
            this.horMin = horMax;
            this.horMax = horMin;
        }
        if (verMin <= verMax) {
            verSign = +1;
            this.verMin = verMin;
            this.verMax = verMax;
        } else {
            verSign = -1;
            this.verMin = verMax;
            this.verMax = verMin;
        }
        viewportChanged();
    }

    void viewportChanged() {
        if (horSign != 0 && verSign != 0) {
            double horRng = horMax - horMin;
            double verRng = verMax - verMin;
            if (xMin == xMax || yMin == yMax) {
                delta = Math.max((xMax - xMin) / verRng, (yMax - yMin) / horRng) / 1e6;
            } else {
                delta = Math.min((xMax - xMin) / horRng, (yMax - yMin) / verRng) / 1e6;
            }
            scale = Math.max(delta, Math.min(horRng / (xMax - xMin + delta), verRng / (yMax - yMin + delta)));
            horOffset = (xMin + xMax + delta * horSign) / 2 * scale * horSign - horRng / 2 - horMin;
            verOffset = (yMin + yMax + delta * verSign) / 2 * scale * verSign - verRng / 2 - verMin;
            if (graphSize <= 0) {
                p = null;
            } else {
                p = new FloatingPoint[graphSize + 1];
                for (int i = 0, j = 1; i < graphSize; i = j++) {
                    p[j] = getPoint(coordinate[i][0], coordinate[i][1]);
                }
            }
        }
    }

    public FloatingPoint getPoint(double x, double y) {
        FloatingPoint point = new FloatingPoint();
        point.x = Math.round((x * scale * horSign - horOffset - horMin) / delta) * delta + horMin;
        point.y = Math.round((y * scale * verSign - verOffset - verMin) / delta) * delta + verMin;
        return point;
    }

    public FloatingPoint getCoordinate(double px, double py) {
        FloatingPoint point = new FloatingPoint();
        point.x = (px + horOffset) / scale * horSign;
        point.y = (py + verOffset) / scale * verSign;
        return point;
    }

    public FloatingPoint[] getBoundingBox() {
        FloatingPoint[] box = new FloatingPoint[2];
        box[0] = getPoint(xMin, yMin);
        box[1] = getPoint(xMax, yMax);
        return box;
    }

    public int getGraphSize() {
        return graphSize;
    }

    public FloatingPoint getCoordinatePoint(int n) {
        return p[n];
    }

    public void paintGraph() {
        if(highlightPentagons && !pentagonsAlreadyDetermined)
            determinePentagons();

        device.beginGraph();
        device.beginEdges();
        for (int i = graphSize; i > 0; --i) {
            EdgeIterator it = graph.getEdgeIterator(i);
            while (it.hasNext()) {
                int j = it.nextEdge();
                if (j >= i) {
                    continue; // draw only edges to vertices that aren't drawn yet
                }

                device.paintEdge(p[i].x, p[i].y, p[j].x, p[j].y, i, j,
                        highlightPentagons && isPartOfPentagon[i][j]);
            }
        }
        device.beginVertices();
        for (int i = graphSize; i > 0; --i) {
            device.paintVertex(p[i].x, p[i].y, i);
        }
        
    }
}
