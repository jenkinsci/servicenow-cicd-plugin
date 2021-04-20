// Javascript code used by ServiceNow builders.
// Created: April, 2021

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

const InstanceScanBuilder = {
    adjustVisibility: (builderId, scanType) => {
        switch(scanType) {
            case 'fullScan':
                InstanceScanBuilder.disableAll(builderId);
                break;
            case 'pointScan':
                InstanceScanBuilder.enablePointScan(builderId);
                break;
            case 'scanWithCombo':
                InstanceScanBuilder.enableComboScan(builderId);
                break;
            case 'scanWithSuiteOnScopedApps':
            case 'scanWithSuiteOnUpdateSets':
                InstanceScanBuilder.enableSuiteScan(builderId);
                break;
            default:
                // the same as for fullScan
                InstanceScanBuilder.disableAll(builderId);
        }
    },

    disableAll: (builderId) => {
        Visibility.hide(document.getElementById(builderId + '-targetTable'));
        Visibility.hide(document.getElementById(builderId + '-targetRecord'));
        Visibility.hide(document.getElementById(builderId + '-targetCombo'));
        Visibility.hide(document.getElementById(builderId + '-suite'));
        Visibility.hide(document.getElementById(builderId + '-requestBody'));
    },

    enableAll: (builderId) => {
        Visibility.show(document.getElementById(builderId + '-targetTable'));
        Visibility.show(document.getElementById(builderId + '-targetRecord'));
        Visibility.show(document.getElementById(builderId + '-targetCombo'));
        Visibility.show(document.getElementById(builderId + '-suite'));
        Visibility.show(document.getElementById(builderId + '-requestBody'));
    },
    enablePointScan: (builderId) => {
        Visibility.show(document.getElementById(builderId + '-targetTable'));
        Visibility.show(document.getElementById(builderId + '-targetRecord'));
        Visibility.hide(document.getElementById(builderId + '-targetCombo'));
        Visibility.hide(document.getElementById(builderId + '-suite'));
        Visibility.hide(document.getElementById(builderId + '-requestBody'));
    },
    enableComboScan: (builderId) => {
        Visibility.hide(document.getElementById(builderId + '-targetTable'));
        Visibility.hide(document.getElementById(builderId + '-targetRecord'));
        Visibility.show(document.getElementById(builderId + '-targetCombo'));
        Visibility.hide(document.getElementById(builderId + '-suite'));
        Visibility.hide(document.getElementById(builderId + '-requestBody'));
    },
    enableSuiteScan: (builderId) => {
        Visibility.hide(document.getElementById(builderId + '-targetTable'));
        Visibility.hide(document.getElementById(builderId + '-targetRecord'));
        Visibility.hide(document.getElementById(builderId + '-targetCombo'));
        Visibility.show(document.getElementById(builderId + '-suite'));
        Visibility.show(document.getElementById(builderId + '-requestBody'));
    }
}

const Visibility = {
    hide: (element) => {
        Visibility.setVisibility(element, false);
    },
    show: (element) => {
        Visibility.setVisibility(element, true);
    },
    setVisibility: (element, visible) => {
        visible = visible || false;
        if(element) {
            if(element.parentElement && element.parentElement.parentElement) {
                element.parentElement.parentElement.style.display = visible ? '' : 'none';
            }
            else {
                console.log("Element enclosing the row with field not found for " + element.id);
            }
        }

    }
}

