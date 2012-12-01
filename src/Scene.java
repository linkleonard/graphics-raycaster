/* class Scene
 * Provides the structure for what is in the scene, and contains classes
 * for their rendering
 *
 * Doug DeCarlo
 */
import java.util.*;
import java.text.ParseException;
import java.lang.reflect.*;
import java.io.*;
import javax.vecmath.*;

class Scene
{
    // Scene elements
    Vector<Shape>    objects    = new Vector<Shape>();
    Vector<Light>    lights     = new Vector<Light>();
    Vector<Material> materials  = new Vector<Material>();
    Camera      camera     = null;
    MatrixStack MStack     = new MatrixStack();

    RGBImage    image      = null;

    // ------
    
    // Current insertion point in hierarchy for parser
    Vector<Shape> currentLevel;

    // Hierarchy enable (if off, "up" and "down" have no effect)
    // (Use this if you implement hierarchical object management or CSG)
    // (if you turn this on, you'll need to re-write intersects() and 
    // shadowTint() recursively for it to see the child objects!)
    boolean hierarchyOn    = false;
    
    // ------

    // Maximum recursion depth for a ray
    double recursionDepth  = 3;
    
    // Minimum t value in intersection computations
    double epsilon         = 1e-5;
    
    // Constructor
    public Scene()
        throws ParseException, IOException, NoSuchMethodException,
        ClassNotFoundException,IllegalAccessException,
        InvocationTargetException
    {
        // Set hierarchy at top level
        currentLevel = objects;

        // Add default material
        materials.addElement(new Material("default"));
    }

    //-----------------------------------------------------------------------

    /** render an image of size width X height */
    public RGBImage render(int width, int height, boolean verbose)
        throws ParseException, IOException, NoSuchMethodException,
        ClassNotFoundException,IllegalAccessException,
        InvocationTargetException
    {
        // Set up camera for this image resolution
        camera.setup(width, height);

        // Make a new image
        image = new RGBImage(width, height);

        // Ray trace every pixel -- the main loop
        for (int i = 0; i < image.getWidth(); i++) {
            if (verbose)
              System.out.print("Rendering " +
                               (int)(100.0*i/(image.getWidth()-1)) + "%\r");

            for (int j = 0; j < image.getHeight(); j++) {
                // Compute (x,y) coordinates of pixel in [-1, 1]
                double x = ((double)i)/(image.getWidth()  - 1) * 2 - 1;
                double y = ((double)j)/(image.getHeight() - 1) * 2 - 1;
	       
                // Compute ray at pixel (x,y)
                Ray r = camera.pixelRay(x, y);
	       
                // Compute resulting color at pixel (x,y)
                Vector3d color = castRay(r, 0);
	       
                // Set color in image
                image.setPixel(i,j, color);
            }
        }

        if (verbose) {
            System.out.println();
            System.out.println("Done!");
        }

        return image;
    }

    /** compute pixel color for ray tracing computation for ray r
     *  (at a recursion depth)
     */
    private Vector3d castRay(Ray r, int depth)
    {
        Vector3d color = new Vector3d();
        ISect isect = new ISect();

        // Check if the ray hit any object (or recursion depth was exceeded)
        if (depth <= recursionDepth && intersects(r, isect)) {
            // -- Ray hit object as specified in isect

            Material mat = isect.getHitObject().getMaterialRef();

            // -- Compute contribution to this pixel for each light by doing
            //    the lighting computation there (sending out a shadow feeler
            //    ray to see if light is visible from intersection point)

            // ...

            // -- Call castRay() recursively to handle contribution
            //    from reflection and refraction

            // ...

            // Placeholder (just use diffuse color)
            color.set(mat.getKd());
        }

        return color;
    }

    /** determine the closest intersecting object along ray r (if any) 
     *  and its intersection point
     */
    private boolean intersects(Ray r, ISect intersection)
    {
        // ...

        // For each object
        Enumeration e = objects.elements();
        while (e.hasMoreElements()) {
            Shape current = (Shape)e.nextElement();

            // ...

            // Find closest intersection point

            // ...
        }
       
        if (intersection.getHitObject() != null) {
            // Transform intersection into world space

            // ...
        }

        return false;
    }

    /** compute the amount of unblocked color that is let through to
     *  a given intersection, for a particular light
     *
     *  If the light is entirely blocked, return (0,0,0), not blocked at all
     *  return (1,1,1), and partially blocked return the product of Kt's
     *  (from transparent objects)
     */
    Vector3d shadowRay(ISect intersection, Light light)
    {
        // ...

        // Compute shadow ray and call shadowTint() or shadowTintDirectional()

        // ...

        // Placeholder (not blocked)
        return new Vector3d(1,1,1);
    }

    /** determine how the light is tinted along a particular ray which
     *  has no maximum distance (i.e. from a directional light)
     */
    private Vector3d shadowTintDirectional(Ray r)
    {
        return shadowTint(r, Double.MAX_VALUE);
    }

    /** determine how the light is tinted along a particular ray, not
     *  considering intersections further than maxT
     */
    private Vector3d shadowTint(Ray r, double maxT)
    {
        Vector3d tint = new Vector3d(1.0, 1.0, 1.0);

        // ...

        // For each object
        Enumeration e = objects.elements();
        while (e.hasMoreElements()) {
            Shape current = (Shape)e.nextElement();

            // ...

            // ... find product of Kt values that intersect this ray
        }

        return tint;
    }

    //------------------------------------------------------------------------

    /** Fetch a material by name */
    Material getMaterial(String name)
    {
        // Unspecified material gets default
        if (name == null || name.length() == 0)
          name = new String("default");

        // Find the material with this name
        for (int i = 0; i < materials.size(); i++){
            Material mat = (Material)materials.elementAt(i);

            if (mat.getName().compareTo(name) == 0) {
                return mat;
            }
        }

        throw new RuntimeException("Undefined material " + name);
    }

    /** Add a new scene element */
    public void addObject(RaytracerObject newItem)
    {
        if (newItem instanceof Light) {
            Light l = (Light)newItem;

            l.transform(MStack.peek());

            lights.addElement(l);
        } else if (newItem instanceof Material) {
            Material m = (Material)newItem;

            materials.addElement(m);
        } else if (newItem instanceof Shape) {
            Shape s = (Shape)newItem;

            s.parent = currentLevel;
            s.setMaterialRef(getMaterial(s.getMaterialName()));
            s.setMatrix(MStack.peek());

            currentLevel.addElement(s);
        }
        else if (newItem instanceof Camera){
            camera = (Camera)newItem;
        }
    }

    /** Set up the scene (called after the scene file is read in) */
    public void setup()
        throws ParseException, IOException, NoSuchMethodException,
        ClassNotFoundException,IllegalAccessException,
        InvocationTargetException
    {
        // Specify default camera if none specified in scene file
        if (camera == null)
          camera = new Camera();

        // Set up materials
        for (int i = 0; i < materials.size(); i++){
            Material mat = (Material)materials.elementAt(i);
            mat.setup(Trace.verbose);
        }
    }

    //-------------------------------------------------------------------------

    // accessors
    public RGBImage getImage() { return image; }
    public void setImage(RGBImage newImage) { image = newImage; }
    public MatrixStack getMStack()  { return MStack; }
}
