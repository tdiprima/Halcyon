export function colorPalette() {
  let paletteElement = document.createElement('select');
  paletteElement.id = 'colorPalette';
  document.body.insertBefore(paletteElement, document.querySelector('canvas'));

  // window.useriri = '/zephyr/colorclasses.json';
  if (!window.useriri) {
    buildColorPalette(paletteElement);
  } else {
    fetch(window.useriri, {
      method: 'GET',
      headers: {
        'Accept': 'application/ld+json'
      }
    })
      .then(response => {
        if (!response.ok) {
          throw new Error('Network response was not ok. Status code:' + response.status);
        }
        return response.json();
      })
      .then(data => {
        buildColorPalette(paletteElement, data);
      })
      .catch(error => {
        console.error('There was a problem with the fetch operation:', error);
        buildColorPalette(paletteElement);
      });
  }
}

function buildColorPalette(paletteElement, data) {
  let options;
  if (data) {
    options = [
      {value: 'nothing', text: '-- Class --'}
    ];

    // Create and append the options to the dropdown
    data.hasAnnotationClass.forEach(annotationClass => {
      const color = annotationClass.color;
      const name = annotationClass.hasClass.name;
      options.push({value: `${color}:${name}`, text: name});
    });
  } else {
    options = [
      {value: 'nothing', text: '-- Class --'},
      {value: '#0f4d0f:Tumor', text: 'Tumor'},
      {value: '#ff0000:Lymphocyte', text: 'Lymphocyte'},
      {value: '#a700b0:Misc', text: 'Misc'},
      {value: '#0000ff:Background', text: 'Background'}
    ];
  }

  options.forEach(opt => {
    const option = document.createElement('option');
    option.value = opt.value;
    option.textContent = opt.text;
    paletteElement.appendChild(option);
  });

  paletteElement.addEventListener('change', (event) => {
    if (event.target.value === 'nothing') {
      window.cancerColor = '';
      window.cancerType = '';
    } else {
      let arr = event.target.value.split(':');
      window.cancerColor = arr[0];
      window.cancerType = arr[1];
    }
  });
}

export function getColorAndType() {
  let color, type;
  // Set the color and type before starting to draw
  if (window.cancerColor && window.cancerColor.length > 0) {
    color = window.cancerColor;
    type = window.cancerType;
  } else {
    color = "#0000ff";
    type = "";
  }
  return {color, type};
}
