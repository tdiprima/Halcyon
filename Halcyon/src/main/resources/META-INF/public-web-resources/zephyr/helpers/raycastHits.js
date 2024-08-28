/**
 * Raycasting Object Hit Order
 * Raycasts on a mouse click and returns an array of objects hit, sorted by the order they are hit (from closest to furthest)
 */
import * as THREE from 'three';

export function getRaycastHits(scene, camera, renderer) {
  // Create a raycaster
  const raycaster = new THREE.Raycaster();
  const mouse = new THREE.Vector2();

  // Event listener for mouse click
  renderer.domElement.addEventListener('click', (event) => {
    // Calculate mouse position in normalized device coordinates (-1 to +1)
    mouse.x = (event.clientX / renderer.domElement.clientWidth) * 2 - 1;
    mouse.y = -(event.clientY / renderer.domElement.clientHeight) * 2 + 1;

    // Update the raycaster with the camera and mouse position
    raycaster.setFromCamera(mouse, camera);

    // Raycast against the objects in the scene
    const intersects = raycaster.intersectObjects(scene.children, true);

    // Log the order of intersected objects
    if (intersects.length > 0) {
      console.log("Hit objects (from closest to furthest):");
      intersects.forEach((intersect, index) => {
        console.log(`Hit ${index + 1}:`, intersect.object.name);
      });
    } else {
      console.log("No hits detected.");
    }
  });
}
