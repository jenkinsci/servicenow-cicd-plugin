// Javascript code used by ServiceNow builders.
// Created: 04.2021

const BatchInstallBuilder = {
    adjustVisibility: (builderId, useFile) => {
        useFile = useFile || false;
        if (useFile) {
            Visibility.setVisibility(document.querySelector('#' + builderId + '-file'), true);
            Visibility.setVisibility(document.querySelector('#' + builderId + '-batchName'), false);
            Visibility.setVisibility(document.querySelector('#' + builderId + '-packages'), false);
            Visibility.setVisibility(document.querySelector('#' + builderId + '-notes'), false);
        } else {
            Visibility.setVisibility(document.querySelector('#' + builderId + '-file'), false);
            Visibility.setVisibility(document.querySelector('#' + builderId + '-batchName'), true);
            Visibility.setVisibility(document.querySelector('#' + builderId + '-packages'), true);
            Visibility.setVisibility(document.querySelector('#' + builderId + '-notes'), true);
        }
    }
}

const Visibility = {
    setVisibility: (element, visible) => {
        visible = visible || false;
        element.parentElement.parentElement.style.display = visible ? 'inherit' : 'none';
    }
}

