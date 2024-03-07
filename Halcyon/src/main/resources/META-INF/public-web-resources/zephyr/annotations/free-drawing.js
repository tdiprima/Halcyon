/**
 * Allows user to draw on an image.
 * Raycasting target meshes are the squares that rapture.js creates.
 */
import * as THREE from 'three';
import { createButton, textInputPopup, deleteIcon } from "../helpers/elements.js";
import { worldToImageCoordinates } from "../helpers/conversions.js";

export function enableDrawing(scene, camera, renderer, controls) {
  let btnDraw = createButton({
    id: "toggleButton",
    innerHtml: "<i class=\"fas fa-pencil-alt\"></i>",
    title: "free-draw"
  });

  let isDrawing = false;
  let mouseIsPressed = false;
  let color = "#0000ff";

  btnDraw.addEventListener("click", function () {
    if (isDrawing) {
      isDrawing = false;
      controls.enabled = true;
      this.classList.replace('btnOn', 'annotationBtn');

      // Remove the mouse event listeners
      renderer.domElement.removeEventListener("mousemove", onMouseMove);
      renderer.domElement.removeEventListener("mouseup", onMouseUp);
      renderer.domElement.removeEventListener("pointerdown", onPointerDown);
    } else {
      // Drawing on
      isDrawing = true;
      controls.enabled = false;
      this.classList.replace('annotationBtn', 'btnOn');

      // Set up the mouse event listeners
      renderer.domElement.addEventListener("mousemove", onMouseMove);
      renderer.domElement.addEventListener("mouseup", onMouseUp);
      renderer.domElement.addEventListener("pointerdown", onPointerDown);
    }
  });

  // Set up the raycaster and mouse vector
  let raycaster = new THREE.Raycaster();
  let mouse = new THREE.Vector2();

  let lineMaterial = new THREE.LineBasicMaterial({ color, linewidth: 5 });

  // Dashed Line Issue Solution
  lineMaterial.polygonOffset = true; // Prevent z-fighting (which causes flicker)
  lineMaterial.polygonOffsetFactor = -1; // Push the polygon further away from the camera
  lineMaterial.depthTest = false;  // Render on top
  lineMaterial.depthWrite = false; // Object won't be occluded
  lineMaterial.transparent = true; // Material transparent
  lineMaterial.alphaTest = 0.5;    // Pixels with less than 50% opacity will not be rendered

  let line;
  let currentPolygonPositions = []; // Store positions for current polygon
  let polygonPositions = []; // Store positions for each polygon
  const distanceThreshold = 0.1;
  let objects = [];

  function onPointerDown(event) {
    if (isDrawing) {
      mouseIsPressed = true;

      // Build the objects array
      objects = [];
      scene.traverse(function (object) {
        if (object instanceof THREE.Mesh && object.visible) {
          objects.push(object);
        }
      });

      // Create a new BufferAttribute for each line
      line = new THREE.Line(new THREE.BufferGeometry(), lineMaterial);
      line.name = "free-draw annotation";
      scene.add(line);

      currentPolygonPositions = []; // Start a new array for the current polygon's positions
    }
  }

  function onMouseMove(event) {
    if (isDrawing && mouseIsPressed) {
      // Get the bounding rectangle of the renderer's DOM element
      const rect = renderer.domElement.getBoundingClientRect();

      // Adjust the mouse coordinates
      mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
      mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

      raycaster.setFromCamera(mouse, camera);

      // These are all the squares
      let intersects = raycaster.intersectObjects(objects, true);

      if (intersects.length > 0) {
        const intersect = intersects[0];
        let point = intersect.point;
        // console.log("intersect object scale:", intersect.object.scale);

        // Check if it's the first vertex of the current polygon
        const isFirstVertex = currentPolygonPositions.length === 0;

        if (isFirstVertex) {
          currentPolygonPositions.push(point.x, point.y, point.z);
        } else {
          // DISTANCE CHECK
          const lastVertex = new THREE.Vector3().fromArray(currentPolygonPositions.slice(-3));
          const currentVertex = new THREE.Vector3(point.x, point.y, point.z);
          const distance = lastVertex.distanceTo(currentVertex);

          if (distance > distanceThreshold) {
            currentPolygonPositions.push(point.x, point.y, point.z); // Store the position in the current polygon's array
            line.geometry.setAttribute("position", new THREE.Float32BufferAttribute(currentPolygonPositions, 3)); // Use the current polygon's array for the line's position attribute
          }
        }

        if (line.geometry.attributes.position) {
          line.geometry.attributes.position.needsUpdate = true;
        }
      }
    }
  }

  function onMouseUp(event) {
    if (isDrawing && mouseIsPressed) {
      mouseIsPressed = false;

      // Ensure there are at least 3 points to form a closed polygon
      if (currentPolygonPositions.length >= 9) { // 3 points * 3 coordinates (x, y, z)
        // Close the polygon by adding the first point to the end
        const firstPoint = currentPolygonPositions.slice(0, 3);
        currentPolygonPositions.push(...firstPoint);

        // Create a new geometry with the closed polygon positions
        const closedPolygonGeometry = new THREE.BufferGeometry();
        closedPolygonGeometry.setAttribute('position', new THREE.Float32BufferAttribute(currentPolygonPositions, 3));
        line.geometry = closedPolygonGeometry;
        line.geometry.setDrawRange(0, currentPolygonPositions.length / 3);
        line.geometry.computeBoundingSphere();
      }

      polygonPositions.push(currentPolygonPositions); // Store the current polygon's positions

      // toImageCoords(currentPolygonPositions, scene);
      // deleteIcon(event, line, scene);

      textInputPopup(event, line);

      // console.log("line:", line);

      currentPolygonPositions = []; // Clear the current polygon's array for the next drawing
    }
  }

  function toImageCoords(currentPolygonPositions) {
    console.log("line geometry positions:\n", currentPolygonPositions);
    const imgCoords = worldToImageCoordinates(currentPolygonPositions, scene);
    console.log("Image coordinates:", imgCoords);
  }
}
