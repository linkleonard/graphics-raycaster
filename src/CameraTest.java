import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.junit.Before;
import org.junit.Test;


public class CameraTest {
	Camera camera;
	
	@Before
	public void setUp() throws Exception {
		camera = null;
		try {
			camera = new Camera();
		} catch (NoSuchMethodException | ClassNotFoundException
				| IllegalAccessException | InvocationTargetException
				| ParseException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        camera.setup(640, 480);
	}
	
	
	boolean isVectorNormalized(Vector3d vec) {
		// We use a range test because of precision errors
    	// If the squared length is in range, then the length must be in range.
		double minlen = 0.999999999999999;
    	double maxlen = 1.000000000000001;
    	
		return (minlen < vec.lengthSquared() && 
				vec.lengthSquared() < maxlen);
	}
	
	void originRayTest() {
		// Test the ray generation to make sure it works correctly
        Ray originRay = camera.pixelRay(0, 0);
        Point3d expectedOrigin = new Point3d(0, 0, -camera.near);
        Vector3d expectedDirection = new Vector3d(0, 0, -1);
        assert(originRay.origin.equals(expectedOrigin));
        assert(originRay.direction.equals(expectedDirection));
		
    	// This must be normalized
    	assert(isVectorNormalized(originRay.direction));
    	
    	// Ray should be on the near plane
    	assert(originRay.origin.z == -camera.near);
	}
	
	@Test
	public void testPixelRay() {
        originRayTest();
        
        camera.near = 0.5;
        originRayTest();
        
        camera.near = 2.0;
        originRayTest();
        
        
	}

}
