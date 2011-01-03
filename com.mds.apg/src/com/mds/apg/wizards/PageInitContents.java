/*
 * Copyright (C) 2010-2011 Mobile Developer Solutions
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mds.apg.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import java.io.File;

public final class PageInitContents extends WizardSection{
    
    // Set up storage for persistent initializers

    private final static String SOURCE_DIR = com.mds.apg.Activator.PLUGIN_ID + ".source"; 
    private final static String USE_EXAMPLE_DIR = com.mds.apg.Activator.PLUGIN_ID + ".example";

    /** Last user-browsed location */
    private String mCustomLocationOsPath;  
    private boolean mUseFromExample;
    
    // widgets

    private Button mBrowseButton;
    private Label mLocationLabel;
    private Text mLocationPathField;
    protected Button mWithJqm;

    PageInitContents(AndroidPgProjectCreationPage wizardPage, Composite parent) {
        super(wizardPage);
        mCustomLocationOsPath = doGetPreferenceStore().getString(SOURCE_DIR);  
        mUseFromExample = doGetPreferenceStore().getString(USE_EXAMPLE_DIR) != "" ; // returns false if unset
        createGroup(parent);
    }
    
    /**
     * Creates the group for the Project options:
     * [radio] Use example source from phonegap directory
     * [radio] Create project from existing sources
     * Location [text field] [browse button]
     *
     * @param parent the parent composite
     */
    protected final void createGroup(Composite parent) {
        Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
        group.setLayout(new GridLayout(2, /* num columns */ false /* columns of not equal size */));
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setFont(parent.getFont());
        group.setText("Project Contents");
        mWizardPage.mContentsSection = group; // Visibility can be adjusted by other widgets

        boolean initialVal = isCreateFromExample();
        final Button createFromExampleRadio = new Button(group, SWT.RADIO);
        createFromExampleRadio.setText("Use phonegap example source as template for project");
        createFromExampleRadio.setSelection(initialVal);
        createFromExampleRadio.setToolTipText("Populate your project with the example shipped with your phonegap installation");

        boolean jqmInit = mWizardPage.mJqmDialog.jqmChecked();
        mWithJqm = new Button(group, SWT.CHECK);
        mWithJqm.setText("with jQuery Mobile");
        mWithJqm.setSelection(jqmInit);
        mWithJqm.setVisible(jqmInit);
        mWithJqm.setEnabled(initialVal);
        mWithJqm.setToolTipText("Create a PhoneGap example project that uses jQuery Mobile");
        
        SelectionListener withListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                mWizardPage.validatePageComplete();
            }
        };        
        mWithJqm.addSelectionListener(withListener);  // to update directory
        
        Button existing_project_radio = new Button(group, SWT.RADIO);
        existing_project_radio.setText("Create project from specified source directory");
        existing_project_radio.setToolTipText("Specify root directory containing your sources that you wish to populate into the Android project"); 
        existing_project_radio.setSelection(!initialVal);

        SelectionListener location_listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                boolean newVal = createFromExampleRadio.getSelection();
                mUseFromExample = newVal;
                doGetPreferenceStore().setValue(USE_EXAMPLE_DIR, newVal ? "true" : "");
                mWithJqm.setEnabled(newVal);
                mWizardPage.validatePageComplete();
            }
        };

        createFromExampleRadio.addSelectionListener(location_listener);
        existing_project_radio.addSelectionListener(location_listener);
        
        Composite location_group = new Composite(parent, SWT.NONE);
        location_group.setLayout(new GridLayout(3, /* num columns */
                false /* columns of not equal size */));
        location_group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        location_group.setFont(parent.getFont());

        mLocationLabel = new Label(location_group, SWT.NONE);
        mLocationLabel.setText("Location:");
        mLocationPathField = new Text(location_group, SWT.BORDER);       
        mBrowseButton = setupDirectoryBrowse(mLocationPathField, parent, location_group);
        enableLocationWidgets(!initialVal);  
    }

    // --- Internal getters & setters ------------------

    @Override
    final String getValue() {
        return mLocationPathField == null ? "" : mLocationPathField.getText().trim(); //$NON-NLS-1$
    }
    
    @Override
    final Text getRawValue() {
        return mLocationPathField; 
    }
    
    @Override
    final String getLocationSave() {
        return mCustomLocationOsPath;
    }
    
    @Override
    final void setLocationSave(String s) {
        mCustomLocationOsPath = s;
    }

    /** Returns the value of the "Create from Existing Sample" radio. */
    /* TODO - Simplify like senchaChecked */
    
    protected boolean isCreateFromExample() {
        return mUseFromExample;
    }
    
    void locationVisibility(boolean v) {
        mLocationLabel.setVisible(v);
        mLocationPathField.setVisible(v);
    }

    // --- UI Callbacks ----
    
    /**
     * Enables or disable the location widgets depending on the user selection:
     * the location path is enabled when using the "existing source" mode (i.e. not new project)
     * or in new project mode with the "use default location" turned off.
     */
    void enableLocationWidgets(boolean locationEnabled) {
        mLocationLabel.setEnabled(locationEnabled);
        mLocationPathField.setEnabled(locationEnabled);
        mBrowseButton.setVisible(locationEnabled);
    }
    
    protected void enableLocationWidgets() {
        enableLocationWidgets(!mUseFromExample);
    }
        
    /**
     * Validates the location path field.
     *
     * @return The wizard message type, one of MSG_ERROR, MSG_WARNING or MSG_NONE.
     */
    protected int validate() {
        File locationDir = new File(getValue());
        if (!locationDir.exists() || !locationDir.isDirectory()) {
            return mWizardPage.setStatus("A valid directory name containing an index.html must be specified in the Location field.", 
                AndroidPgProjectCreationPage.MSG_ERROR);
        } else {
            String[] l = locationDir.list();
            if (l.length == 0) {
                return mWizardPage.setStatus("The location directory is empty. It should include the source to populate the project", 
                        AndroidPgProjectCreationPage.MSG_ERROR);
            }
            boolean foundIndexHtml = false;

            for (String s : l) {
                if (s.equals("index.html")) {
                    foundIndexHtml = true;
                    break;
                }
            }
            if (!foundIndexHtml) {
                return mWizardPage.setStatus("The location directory must include an index.html file", 
                        AndroidPgProjectCreationPage.MSG_ERROR);
            }
            // TODO more validation
            
            // We now have a good directory, so set example path and save value
            doGetPreferenceStore().setValue(SOURCE_DIR, getValue()); 
            return AndroidPgProjectCreationPage.MSG_NONE;
        }
    }
}