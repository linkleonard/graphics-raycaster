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

import javax.swing.text.Position;
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
    
    // Diagonal Color Matrix
    // For the image point (0, 0), these are the following diagonal values:
    // Top-left: 0, 0
    // Top-right: 0, 1
    // Bottom-Left: 1, 0
    // Bottom-Right: 1, 1
    Vector3d[][] diagonals;
    
    double colorDifferenceSquared = 0.025;
    
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
    public RGBImage render(int width, int height, boolean verbose, boolean adaptive)
        throws ParseException, IOException, NoSuchMethodException,
        ClassNotFoundException,IllegalAccessException,
        InvocationTargetException
    {
    	
        // Set up camera for this image resolution
        camera.setup(width, height);

        // Make a new image
        image = new RGBImage(width, height);

        // Prepare the diagonal color matrix
        diagonals = new Vector3d[width + 1][height + 1];
        
        // Ray trace every pixel -- the main loop
        for (int i = 0; i < image.getWidth(); i++) {
            if (verbose)
              System.out.print("Rendering " +
                               (int)(100.0*i/(image.getWidth()-1)) + "%\r");

            for (int j = 0; j < image.getHeight(); j++) {
                // Compute resulting color at pixel (x,y)
                // Set color in image
                image.setPixel(i,j, initialAdaptiveCastRay(i, j, 0, 0.5, adaptive));
            }
        }

        if (verbose) {
            System.out.println();
            System.out.println("Done!");
        }

        return image;
    }

    private Ray calculatePixelRay(double i, double j) {
        // Compute ray at pixel (x,y)
        return camera.pixelRay(
        		((double)i)/(image.getWidth()  - 1) * 2 - 1, 
        		((double)j)/(image.getHeight() - 1) * 2 - 1);
    }
    
    /*
     * Adaptively casts ray for depth = 0.
     */
    private Vector3d initialAdaptiveCastRay(double i, double j, int depth, double delta, boolean adaptive) {
    	if (!adaptive) {
	    	// Compute (x,y) coordinates of pixel in [-1, 1]
	    	Ray r = calculatePixelRay(i, j);
	    	// Compute resulting color at pixel (x,y)
	        return castRay(r, 0, adaptive);
    	} else {
    		Vector3d topLeft, topRight, bottomLeft, bottomRight;
    		
    		// Used for depth == 0 to save diagonal calculation
    		int p = 0, q = 0;
    		if (depth == 0) {
    			p = (int)i;
    			q = (int)j;
    		}
    		
    		// Top Left
    		if (depth > 0)
    			topLeft = castRay(calculatePixelRay(i - delta, j - delta), depth, adaptive);
    		else if (diagonals[p][q] == null) {
    			diagonals[p][q] = topLeft = castRay(calculatePixelRay(i - delta, j - delta), depth, adaptive);
    		} else {
    			topLeft = diagonals[p][q];
    		}
    		
    		// Top Right
    		if (depth > 0)
    			topRight = castRay(calculatePixelRay(i - delta, j + delta), depth, adaptive);
    		else if (diagonals[p][q+1] == null) {
    			diagonals[p][q+1] = topRight = castRay(calculatePixelRay(i - delta, j + delta), depth, adaptive);
    		} else {
    			topRight = diagonals[p][q+1];
    		}
    		
    		// Top Left
    		if (depth > 0)
    			bottomLeft = castRay(calculatePixelRay(i + delta, j - delta), depth, adaptive);
    		else if (diagonals[p+1][q] == null) {
    			diagonals[p+1][q] = bottomLeft = castRay(calculatePixelRay(i + delta, j - delta), depth, adaptive);
    		} else {
    			bottomLeft = diagonals[p+1][q];
    		}
    		
    		// Top Left
    		if (depth > 0)
    			bottomRight = castRay(calculatePixelRay(i + delta, j + delta), depth, adaptive);
    		else if (diagonals[p+1][q+1] == null) {
    			diagonals[p+1][q+1] = bottomRight = castRay(calculatePixelRay(i + delta, j + delta), depth, adaptive);
    		} else {
    			bottomRight = diagonals[p+1][q+1];
    		}
    		
    		Vector3d[] colors = {
    			topLeft,
    			topRight,
    			bottomLeft,
    			bottomRight
    		};
    		
    		if (!areColorsSimilar(colors)) {
//    			return new Vector3d();
    			depth += 1;
    			delta *= 0.5;
    			double nextDelta = delta * 0.5;
    			topLeft = initialAdaptiveCastRay(i - delta, j - delta, depth, nextDelta, adaptive);
    			topRight = initialAdaptiveCastRay(i - delta, j + delta, depth, nextDelta, adaptive);
    			bottomLeft = initialAdaptiveCastRay(i + delta, j - delta, depth, nextDelta, adaptive);
    			bottomRight = initialAdaptiveCastRay(i + delta, j + delta, depth, nextDelta, adaptive);
    		}
    			
    		
    		Vector3d color = new Vector3d(topLeft);
    		color.add(topRight);
    		color.add(bottomLeft);
    		color.add(bottomRight);
    		color.scale(0.25);
    		
    		return color;
        }
    }
    
    
    private boolean areColorsSimilar(Vector3d[] colors) {
    	Vector3d baseColor = new Vector3d();
    	for (int i = 0; i < colors.length; ++i) {
    		for (int j = 0; j < colors.length; ++j) { 
    			if (i != j) {
    				baseColor.set(colors[i]);
    				baseColor.sub(colors[j]);
    				if (baseColor.lengthSquared() > colorDifferenceSquared)
    					return false;
    			}
    		}
    	}
    	return true;
    }
    
    /** compute pixel color for ray tracing computation for ray r
     *  (at a recursion depth)
     */
    private Vector3d castRay(Ray r, int depth, boolean adaptive)
    {
        Vector3d color = new Vector3d();
        ISect isect = new ISect();

        Point3d rayOrigin = new Point3d(r.origin);
        Vector3d rayDirection = new Vector3d(r.direction);
        
        // Check if the ray hit any object (or recursion depth was exceeded)
        if (depth <= recursionDepth && intersects(r, isect)) {
            // -- Ray hit object as specified in isect

            Material mat = isect.getHitObject().getMaterialRef();

            // -- Compute contribution to this pixel for each light by doing
            //    the lighting computation there (sending out a shadow feeler
            //    ray to see if light is visible from intersection point)

            // ...
            for (int i = 0; i < lights.size(); ++i) {
            	Vector3d tint = shadowRay(isect, lights.get(i));
            	
            	// Restore hit object before computing color
            	Vector3d lightColor = lights.get(i).compute(isect, tint, r);
            	color.add(lightColor);
            }
            
            // ==== Reflection Component ====
            // Don't bother reflecting if the object is non reflective
            if (mat.getKs().x != 0 ||
        		mat.getKs().y != 0 ||
        		mat.getKs().z != 0) {
	            // Transform ray to correct direction
	            Tools.reflect(r.direction, rayDirection, isect.getNormal());
	            r.origin.set(isect.getHitPoint());
	            r.direction.negate();
	            
	            Vector3d colorReflect = castRay(r, depth + 1, adaptive);
	            Tools.termwiseMul3d(colorReflect, mat.getKs());
	            color.add(colorReflect);
            }
            
            // ==== RefractionComponent ====
            // Don't bother refracting if the object is opaque
            if (mat.getKt().x != 0 ||
        		mat.getKt().y != 0 ||
        		mat.getKt().z != 0) {
            	r.origin.set(isect.getHitPoint());
	            r.direction.set(rayDirection);
	            
	            // Are we entering? The dot product will be negative
	            if (isect.getNormal().dot(rayDirection) < 0) {
	            	Tools.refract(r.direction, rayDirection, isect.getNormal(), 1, mat.index);
	            	
	            } else {
	            	isect.getNormal().negate();
	            	Tools.refract(r.direction, rayDirection, isect.getNormal(), mat.index, 1);
	            	isect.getNormal().negate();
	            }
	            Vector3d colorRefract = castRay(r, depth + 1, adaptive);
	            Tools.termwiseMul3d(colorRefract, mat.getKt());
	            color.add(colorRefract);
            }
        }

        return color;
    }

    /** determine the closest intersecting object along ray r (if any) 
     *  and its intersection point
     */
    private boolean intersects(Ray r, ISect intersection)
    {
    	ISect closestIntersection = new ISect();
    	closestIntersection.t = Double.MAX_VALUE;
    	
        // For each object
        Enumeration e = objects.elements();
        while (e.hasMoreElements()) {
        	
            Shape current = (Shape)e.nextElement();
            	
            // Transform ray to object space
            Ray copy = new Ray(r);
            current.MInverse.transform(copy.origin);
            current.MInverse.transform(copy.direction);
            
            // Find closest intersection point
        	if (current.hit(copy, intersection, true, epsilon) && intersection.t < closestIntersection.t) {
        		closestIntersection.set(intersection);
        	}
        }
        // Make sure we point to the closest intersection point
        intersection.set(closestIntersection);
        
        if (intersection.getHitObject() != null) {
            // Transform intersection into world space

            // Transform the intersection point
        	intersection.hitObject.M.transform(intersection.hitPoint);
        	
        	// Transform the normal (Make sure its normalized!)
        	intersection.hitObject.MTInverse.transform(intersection.normal);
        	intersection.normal.normalize();
        	
        	return true;
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

    	Vector3d lightDirection = new Vector3d();
    	
    	// This is not a directional light
		if (light.getDirection() == null) {
			// Construct a vector going from Hit Point -> Light
			lightDirection = new Vector3d(light.getPosition());
			lightDirection.sub(intersection.getHitPoint());
			lightDirection.normalize();
		} else {
			// Why don't we need to negate this value?  Isn't this pointing from Light -> Point?
			lightDirection = new Vector3d(light.getDirection());
		}
    	Ray shadow = new Ray(intersection.getHitPoint(), lightDirection);

    	return shadowTint(shadow, intersection.t);
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
        ISect intersection = new ISect();
        while (e.hasMoreElements()) {
            Shape current = (Shape)e.nextElement();

            // ...

            // ... find product of Kt values that intersect this ray
            
            // Transform ray to object space
            Ray copy = new Ray(r);
            current.MInverse.transform(copy.origin);
            current.MInverse.transform(copy.direction);
            copy.direction.normalize();
            
            if (current.hit(copy, intersection, false, epsilon)) {
            	Vector3d kt = new Vector3d(intersection.hitObject.getMaterialRef().getKt());
            	Tools.termwiseMul3d(tint, kt);
            }
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
