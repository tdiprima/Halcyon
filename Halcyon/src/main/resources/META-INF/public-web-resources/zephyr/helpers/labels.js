import * as THREE from "three";
import { createButton, textInputPopup, turnOtherButtonsOff, displayAreaAndPerimeter } from "./elements.js";
import {calculatePolygonArea, calculatePolygonPerimeter} from "./conversions.js"

// Label or area and perimeter
export function label(scene, camera, renderer, controls, originalZ, type) {
  let button;

  if (type === "label") {
    button = createButton({
      id: "label",
      innerHtml: "<i class=\"fas fa-tag\"></i>",
      title: "Label"
    });
  } else {
    button = createButton({
      id: "area",
      innerHtml: "<i class=\"fa fa-area-chart\"></i>",
      title: "Area and Perimeter"
    });
  }

  let clicked = false;
  let mouse = new THREE.Vector2();
  let raycaster = new THREE.Raycaster();
  let objects = [];

  button.addEventListener("click", function () {
    clicked = !clicked;
    if (clicked) {
      // alert("on!");
      turnOtherButtonsOff(button);
      controls.enabled = false;
      this.classList.replace('annotationBtn', 'btnOn');
      getAnnotationObjects();
      renderer.domElement.addEventListener('click', onMouseClick, false);
    } else {
      // alert("off!");
      controls.enabled = true;
      this.classList.replace('btnOn', 'annotationBtn');
      objects = [];
      renderer.domElement.removeEventListener('click', onMouseClick, false);
    }
  });

  function getAnnotationObjects() {
    objects = []; // Clear objects array to avoid duplicates
    scene.traverse((object) => {
      if (object.name.includes("annotation")) {
        objects.push(object);
      }
    });
    // console.log("objects", objects);
  }

  function onMouseClick(event) {
    event.preventDefault();

    // Calculate the distance from the camera to a target point (e.g., the center of the scene)
    const distance = camera.position.distanceTo(scene.position);

    // Adjust the threshold based on the distance
    raycaster.params.Line.threshold = calculateThreshold(distance, 100, 1000); // 200/5500
    // raycaster.params.Line.threshold = 2500;
    // console.log("raycaster line threshold", raycaster.params.Line.threshold);

    const rect = renderer.domElement.getBoundingClientRect();
    mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
    mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;
    raycaster.setFromCamera(mouse, camera);

    try {
      const intersects = raycaster.intersectObjects(objects, true);
      if (intersects.length > 0) {
        // Sort by distance to the camera
        // intersects.sort((a, b) => a.distance - b.distance);
        const selectedMesh = intersects[0].object;
        // console.log("selectedMesh", selectedMesh);

        if (type === "label") {
          textInputPopup(event, selectedMesh);
        } else {
          // Calculate area and perimeter
          let currentPolygonPositions = selectedMesh.geometry.attributes.position.array;
          const area = calculatePolygonArea(currentPolygonPositions, camera, renderer);
          const perimeter = calculatePolygonPerimeter(currentPolygonPositions, camera, renderer);

          // Display the area and perimeter
          displayAreaAndPerimeter(area, perimeter);
        }

      }
      // else {
      //   console.log("nothing");
      // }
    } catch (error) {
      console.error("Intersection error:", error);
    }
  }

  // Helper function to calculate the threshold based on the distance
  const minDistance = 200;
  const maxDistance = originalZ;
  function calculateThreshold(currentDistance, minThreshold, maxThreshold) {
    // Clamp currentDistance within the range
    currentDistance = Math.max(minDistance, Math.min(maxDistance, currentDistance));
    return maxThreshold + (minThreshold - maxThreshold) * (maxDistance - currentDistance) / (maxDistance - minDistance);
  }
}
