import * as THREE from 'three';
import { createButton, textInputPopup, deleteIcon, turnOtherButtonsOff } from "../helpers/elements.js";
import { getMousePosition } from "../helpers/mouse.js";

export function polygon(scene, camera, renderer, controls) {
  const canvas = renderer.domElement;
  let isDrawing = false;
  let mouseIsPressed = false;
  let points = [];
  let currentPolygon = null;
  let longPressTimeout;

  let material = new THREE.LineBasicMaterial({ color: 0x0000ff, linewidth: 5 });
  material.depthTest = false;
  material.depthWrite = false;

  let polygonButton = createButton({
    id: "polygon",
    innerHtml: "<i class=\"fa-solid fa-draw-polygon\"></i>",
    title: "Polygon"
  });

  polygonButton.addEventListener("click", function () {
    if (isDrawing) {
      isDrawing = false;
      controls.enabled = true;
      this.classList.replace('btnOn', 'annotationBtn');
      canvas.removeEventListener("mousedown", onMouseDown, false);
      canvas.removeEventListener("mousemove", onMouseMove, false);
      canvas.removeEventListener("mouseup", onMouseUp, false);
      canvas.removeEventListener("dblclick", onDoubleClick, false);

      canvas.removeEventListener("touchstart", onTouchStart, false);
      canvas.removeEventListener("touchmove", onTouchMove, false);
      canvas.removeEventListener("touchend", onTouchEnd, false);
      canvas.removeEventListener("touchcancel", onTouchCancel, false);
    } else {
      isDrawing = true;
      turnOtherButtonsOff(polygonButton);
      controls.enabled = false;
      this.classList.replace('annotationBtn', 'btnOn');
      canvas.addEventListener("mousedown", onMouseDown, false);
      canvas.addEventListener("mousemove", onMouseMove, false);
      canvas.addEventListener("mouseup", onMouseUp, false);
      canvas.addEventListener("dblclick", onDoubleClick, false);

      canvas.addEventListener("touchstart", onTouchStart, false);
      canvas.addEventListener("touchmove", onTouchMove, false);
      canvas.addEventListener("touchend", onTouchEnd, false);
      canvas.addEventListener("touchcancel", onTouchCancel, false);
    }
  });

  function onMouseDown(event) {
    if (isDrawing && !mouseIsPressed) { // Ensure we start a new polygon
      mouseIsPressed = true;
      points = []; // Reset points for a new polygon
      let point = getMousePosition(event.clientX, event.clientY, canvas, camera);
      points.push(point);
      if (!currentPolygon) { // Create a new LineLoop if there isn't an active one
        currentPolygon = createPolygon();
      }
    }
  }

  function onMouseMove(event) {
    if (isDrawing && mouseIsPressed) {
      let point = getMousePosition(event.clientX, event.clientY, canvas, camera);
      points[points.length - 1] = point;
      updatePolygon();
    }
  }

  function onMouseUp(event) {
    if (isDrawing) {
      let point = getMousePosition(event.clientX, event.clientY, canvas, camera);
      points.push(point);
      updatePolygon();
    }
  }

  function onDoubleClick(event) {
    if (isDrawing && points.length > 2) { // Ensure a valid polygon
      mouseIsPressed = false;
      points.pop(); // Remove the duplicated point from double-click
      finalizeCurrentPolygon(); // Finalize and prepare for a new polygon
      // deleteIcon(event, currentPolygon, scene);
      textInputPopup(event, currentPolygon);
      currentPolygon = null; // Reset currentPolygon for the next one
    }
  }

  function onTouchStart(event) {
    if (isDrawing && !mouseIsPressed) { // Ensure we start a new polygon
      mouseIsPressed = true;
      points = []; // Reset points for a new polygon
      let touch = event.touches[0];
      let point = getMousePosition(touch.clientX, touch.clientY, canvas, camera);
      points.push(point);
      if (!currentPolygon) { // Create a new LineLoop if there isn't an active one
        currentPolygon = createPolygon();
      }

      longPressTimeout = setTimeout(() => {
        if (isDrawing && points.length > 2) {
          onLongPress(event);
        }
      }, 500); // Set long press duration to 500ms
    }
  }

  function onTouchMove(event) {
    if (isDrawing && mouseIsPressed) {
      let touch = event.touches[0];
      let point = getMousePosition(touch.clientX, touch.clientY, canvas, camera);
      points[points.length - 1] = point;
      updatePolygon();
    }
  }

  function onTouchEnd(event) {
    clearTimeout(longPressTimeout);
    if (isDrawing) {
      let touch = event.changedTouches[0];
      let point = getMousePosition(touch.clientX, touch.clientY, canvas, camera);
      points.push(point);
      updatePolygon();
    }
  }

  function onTouchCancel(event) {
    clearTimeout(longPressTimeout);
  }

  function onLongPress(event) {
    if (isDrawing && points.length > 2) { // Ensure a valid polygon
      mouseIsPressed = false;
      points.pop(); // Remove the duplicated point from long press
      finalizeCurrentPolygon(); // Finalize and prepare for a new polygon
      textInputPopup(event, currentPolygon);
      currentPolygon = null; // Reset currentPolygon for the next one
    }
  }

  function finalizeCurrentPolygon() {
    // This function replaces the temporary line drawing with a finalized LineLoop
    updatePolygon(); // Ensure the final point is included
    // No need to create a new object here, as updatePolygon already updates the LineLoop
  }

  function createPolygon() {
    let geometry = new THREE.BufferGeometry();
    // Ensure material and geometry are correctly set up for a LineLoop
    let polygon = new THREE.LineLoop(geometry, material);
    polygon.renderOrder = 999;
    polygon.name = "polygon annotation";
    scene.add(polygon);
    return polygon;
  }

  function updatePolygon() {
    // This function remains largely the same, ensuring the LineLoop's geometry is updated
    if (currentPolygon && points.length > 0) {
      let positions = new Float32Array(points.length * 3);
      for (let i = 0; i < points.length; i++) {
        positions[i * 3] = points[i].x;
        positions[i * 3 + 1] = points[i].y;
        positions[i * 3 + 2] = points[i].z;
      }
      currentPolygon.geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
      currentPolygon.geometry.attributes.position.needsUpdate = true;
      currentPolygon.geometry.setDrawRange(0, points.length);
    }
  }
}
