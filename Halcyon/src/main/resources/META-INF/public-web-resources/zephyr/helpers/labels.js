import * as THREE from "three";
import { createButton, textInputPopup, turnOtherButtonsOff, displayAreaAndPerimeter } from "./elements.js";
import { calculatePolygonArea, calculatePolygonPerimeter } from "./conversions.js"

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
      // innerHtml: "<i class=\"fa fa-area-chart\"></i>",
      // innerHtml: "<i class=\"fa fa-square\"></i>",
      innerHtml: "<i class=\"fa fa-ruler-combined\"></i>",
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
      turnOtherButtonsOff(button);
      controls.enabled = false;
      this.classList.replace('annotationBtn', 'btnOn');
      getAnnotationObjects();
      renderer.domElement.addEventListener('click', onMouseClick, false);
    } else {
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
  }

  function onMouseClick(event) {
    event.preventDefault();

    const rect = renderer.domElement.getBoundingClientRect();
    mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
    mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;
    raycaster.setFromCamera(mouse, camera);

    try {
      const intersects = raycaster.intersectObjects(objects, true);
      if (intersects.length > 0) {
        const selectedMesh = intersects[0].object;

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
    } catch (error) {
      console.error("Intersection error:", error);
    }
  }
}
