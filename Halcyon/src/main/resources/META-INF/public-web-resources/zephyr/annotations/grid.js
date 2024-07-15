import * as THREE from 'three';
import { createButton, removeObject, turnOtherButtonsOff } from "../helpers/elements.js";

export function grid(scene, camera, renderer, controls) {
  const canvas = renderer.domElement;
  let isGridAdded = false;
  let gridLines;
  let gridSquares;
  let isDragging = false;
  let removeMode = false;
  let lastTapTime = 0;

  let gridButton = createButton({
    id: "addGrid",
    innerHtml: "<i class=\"fas fa-border-all\"></i>",
    title: "Grid"
  });

  gridButton.addEventListener("click", function () {
    if (isGridAdded) {
      window.removeEventListener('mousedown', handleMouseDown);
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);

      window.removeEventListener('touchstart', handleTouchStart);
      window.removeEventListener('touchmove', handleTouchMove);
      window.removeEventListener('touchend', handleTouchEnd);

      isDragging = false;
      controls.enabled = true;
      removeGridLines();
      this.classList.replace('btnOn', 'annotationBtn');
    } else {
      window.addEventListener('mousedown', handleMouseDown);
      window.addEventListener('mousemove', handleMouseMove);
      window.addEventListener('mouseup', handleMouseUp);

      window.addEventListener('touchstart', handleTouchStart);
      window.addEventListener('touchmove', handleTouchMove);
      window.addEventListener('touchend', handleTouchEnd);

      controls.enabled = false;
      turnOtherButtonsOff(gridButton);
      addGrid();
      this.classList.replace('annotationBtn', 'btnOn');
    }
    isGridAdded = !isGridAdded; // Toggle the state
  });

  // Define named functions for event handling
  function handleMouseDown(event) {
    isDragging = true;
    colorSquare(event);
  }

  function handleMouseMove(event) {
    if (isDragging) {
      colorSquare(event);
    }
  }

  function handleMouseUp() {
    isDragging = false;
  }

  function handleTouchStart(event) {
    // Handle double-tap to toggle remove mode
    const currentTime = new Date().getTime();
    const tapInterval = currentTime - lastTapTime;
    if (tapInterval < 300 && tapInterval > 0) {
      removeMode = !removeMode;
      alert(`Remove mode: ${removeMode ? 'ON' : 'OFF'}`);
    }
    lastTapTime = currentTime;

    isDragging = true;
    colorSquare(event.touches[0]);
  }

  function handleTouchMove(event) {
    if (isDragging) {
      colorSquare(event.touches[0]);
    }
  }

  function handleTouchEnd() {
    isDragging = false;
  }

  function addGrid() {
    // Create a grid overlay with green lines.
    const gridSize = 50; // Define the size of the grid
    const squareSize = 100; // Define the size of each square in the grid
    gridLines = new THREE.Group(); // Group to hold the grid lines
    gridSquares = new THREE.Group(); // Group to hold the grid squares

    for (let i = 0; i <= gridSize; i++) {
      const lineGeometry = new THREE.BufferGeometry().setFromPoints([
        new THREE.Vector3(i * squareSize - gridSize * squareSize / 2, -gridSize * squareSize / 2, 0),
        new THREE.Vector3(i * squareSize - gridSize * squareSize / 2, gridSize * squareSize / 2, 0)
      ]);
      const lineMaterial = new THREE.LineBasicMaterial({ color: 0x0000ff });
      const line = new THREE.Line(lineGeometry, lineMaterial);
      gridLines.add(line);

      const lineGeometryHorizontal = new THREE.BufferGeometry().setFromPoints([
        new THREE.Vector3(-gridSize * squareSize / 2, i * squareSize - gridSize * squareSize / 2, 0),
        new THREE.Vector3(gridSize * squareSize / 2, i * squareSize - gridSize * squareSize / 2, 0)
      ]);
      const lineHorizontal = new THREE.Line(lineGeometryHorizontal, lineMaterial);
      gridLines.add(lineHorizontal);
    }

    for (let i = 0; i < gridSize; i++) {
      for (let j = 0; j < gridSize; j++) {
        const geometry = new THREE.PlaneGeometry(squareSize, squareSize);
        const material = new THREE.MeshBasicMaterial({ color: 0xff0000, transparent: true, opacity: 0 });
        const square = new THREE.Mesh(geometry, material);

        // Position each square
        square.position.set(i * squareSize - gridSize * squareSize / 2 + squareSize / 2, j * squareSize - gridSize * squareSize / 2 + squareSize / 2, 0);
        square.userData = { colored: false };
        gridSquares.add(square);
      }
    }

    updateGridPosition(); // Calculate and set the initial position of the grid

    gridLines.name = "gridLines";
    gridSquares.name = "gridSquares";
    scene.add(gridLines);
    scene.add(gridSquares);
  }

  function updateGridPosition() {
    if (!gridLines || !gridSquares) return; // If the grid doesn't exist, exit the function

    // Calculate the center of the camera's current view
    const vector = new THREE.Vector3(); // Vector pointing to the center of the screen
    const direction = new THREE.Vector3();
    camera.getWorldDirection(direction);
    vector.addVectors(camera.position, direction.multiplyScalar(1000)); // Adjust distance based on your scene

    // Set grid position to match the calculated center point
    gridLines.position.copy(vector);
    gridLines.position.z = 0; // keep flush
    gridSquares.position.copy(vector);
    gridSquares.position.z = 0; // keep flush
  }

  function removeGridLines() {
    scene.remove(gridLines);
  }

  // Handling Dragging to Color Squares
  const raycaster = new THREE.Raycaster();
  const mouse = new THREE.Vector2();

  function colorSquare(event) {
    const rect = canvas.getBoundingClientRect();

    // Adjust mouse position for canvas offset
    mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
    mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

    // Update the picking ray
    raycaster.setFromCamera(mouse, camera);

    // Calculate objects intersecting the picking ray
    const intersects = raycaster.intersectObjects(gridSquares.children);

    if (intersects.length > 0) {
      const square = intersects[0].object;

      if ((event.shiftKey || removeMode) && square.userData.colored) {
        // Shift-click or double-tap remove mode to un-color the square
        square.material.opacity = 0;
        square.userData.colored = false;
        square.name = "";
      } else if (!square.userData.colored) {
        // Regular drag to color the square
        square.material.opacity = 0.5;
        square.userData.colored = true;
        square.name = "heatmap annotation";
      }
    }
  }
}
