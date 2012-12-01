/* class Camera
 * The camera data structure
 *
 * Doug DeCarlo
 */
import java.io.*;
import java.lang.reflect.*;
import java.text.ParseException;
import javax.vecmath.*;

class Camera extends RaytracerObject
{
    public static String keyword = "camera";

    /** Specification of camera: position, look and up direction */
    Point3d  eye  = new Point3d(0.0, 0.0, 0.0);
    Vector3d look = new Vector3d(0.0, 0.0, -1.0);
    Vector3d up   = new Vector3d(0.0, 1.0, 0.0);

    /** vertical field of view angle */
    double fovy = 50;

    /** near plane */
    double near = 1;

    // ------

    /** aspect ratio (width/height) */
    double aspect;
    
    /** Camera coordinate system (u, v are scaled based on image size) */
    Vector3d u, v, n;

    //-----------------------------------------------------------------------

    /** if the camera is specified by default values, pass a null as tokenizer
     */
    public Camera(StreamTokenizer tokenizer)
        throws ParseException, IOException, NoSuchMethodException,
        ClassNotFoundException,IllegalAccessException,
        InvocationTargetException
    {
        super(tokenizer);

        // add parameters
        addSpec("eye",    "setEye",    eye.getClass().getName());
        addSpec("look",   "setLook",   look.getClass().getName());
        addSpec("up",     "setUp",     up.getClass().getName());
        addSpec("fovy",   "setFovy",   "java.lang.Double");
        addSpec("near",   "setNear",   "java.lang.Double");

        read(tokenizer);
    }

    public Camera()
        throws ParseException, IOException, NoSuchMethodException,
        ClassNotFoundException,IllegalAccessException,
        InvocationTargetException
    {
        super(null);
    }

    /** Set up camera */
    public void setup(int width, int height)
    {
        aspect = (double)width / height;

        computeUVN();
    }

    //-----------------------------------------------------------------------

    // accessors
    public Point3d  getEye()    { return eye; }
    public Vector3d getLook()   { return look; }
    public Vector3d getUp()     { return up; }
    public double   getFovy()   { return fovy; }
    public double   getAspect() { return aspect; }
    public double   getNear()   { return near; }
    
    public void setEye(Point3d newVal)   { eye.set(newVal); }
    public void setLook(Vector3d newVal) { look.set(newVal); }
    public void setUp(Vector3d newVal)   { up.set(newVal); }
    public void setFovy(Double newVal)   { fovy   = newVal.doubleValue(); }
    public void setNear(Double newVal)   { near   = newVal.doubleValue(); }

    //-----------------------------------------------------------------------

    /** create and compute u, v, n from camera specification
     *  n = -look (normalized)
     *  v = the component of up perpendicular to n
     *  u = v x n
     *  (from eye, u and v reach to the extents of the image)
     */
    public void computeUVN()
    {
        n = new Vector3d(look);
        n.negate();
        n.normalize();

        // Length of u and v
        double H = near * Math.tan(fovy/2.0 * Math.PI/180.0);
        double W = H * aspect;

        // v = up - n (up . n)
        v = new Vector3d(n);
        v.scale(-up.dot(n));
        v.add(up);
        v.normalize();
		
        u = new Vector3d(n);
        u.cross(v, n);
        u.normalize();

        // Scale u and v to be size of image
        u.scale(W);
        v.scale(H);
    }

    /** construct ray through pixel (x,y)
     *   - x and y range in [-1, 1] (using u, v coordinates)
     *   - ray origin is on the near plane
     *   - ray direction is normalized version of:
     *         u * x + v * y - n * near 
     *
     *   u, v, n are camera coordinate basis, as computed
     *   by computeUVN(), the origin of which is eye
     */
    public Ray pixelRay(double x, double y)
    {
        // Create and compute ray through pixel
    	Ray ray = new Ray();
    	ray.direction.set(0, 0, 0);
    	ray.direction.x += x * u.x + y * v.x - near * n.x;
    	ray.direction.y += x * u.y + y * v.y - near * n.y;
    	ray.direction.z += x * u.z + y * v.z - near * n.z;
    	ray.direction.normalize();
    	
    	ray.origin.x = 0;
    	ray.origin.y = 0;
    	ray.origin.z = -near;
    	
    	// This must be normalized
    	// We use a range test because of precision errors
    	// If the squared length is in range, then the length must be in range.
    	double minlen = 0.999999999999999;
    	double maxlen = 1.000000000000001;
    	assert(minlen < ray.direction.lengthSquared());
    	assert(ray.direction.lengthSquared() < maxlen);
        return ray;
    }

    public void print(PrintStream out)
    {
        out.println("Eye  : " + eye );
        out.println("Look : " + look);
        out.println("Up   : " + up);
    }
}
