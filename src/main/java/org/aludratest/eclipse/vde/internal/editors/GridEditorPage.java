package org.aludratest.eclipse.vde.internal.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aludratest.eclipse.vde.internal.content.ClipboardRegionDoesNotMatchException;
import org.aludratest.eclipse.vde.internal.content.ClipboardUtil;
import org.aludratest.eclipse.vde.internal.content.PasteStringValueAcceptor;
import org.aludratest.eclipse.vde.internal.model.TestDataConfigurationSegment;
import org.aludratest.eclipse.vde.model.IFieldValue;
import org.aludratest.eclipse.vde.model.IStringListValue;
import org.aludratest.eclipse.vde.model.IStringValue;
import org.aludratest.eclipse.vde.model.ITestData;
import org.aludratest.eclipse.vde.model.ITestDataConfiguration;
import org.aludratest.eclipse.vde.model.ITestDataConfigurationSegment;
import org.aludratest.eclipse.vde.model.ITestDataFieldMetadata;
import org.aludratest.eclipse.vde.model.ITestDataFieldValue;
import org.aludratest.eclipse.vde.model.ITestDataSegmentMetadata;
import org.aludratest.eclipse.vde.model.TestDataFieldType;
import org.databene.commons.ConversionException;
import org.databene.commons.converter.UnsafeConverter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractUiBindingConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.coordinate.Range;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.validate.ContextualDataValidator;
import org.eclipse.nebula.widgets.nattable.data.validate.ValidationFailedException;
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.edit.action.MouseEditAction;
import org.eclipse.nebula.widgets.nattable.edit.config.RenderErrorHandling;
import org.eclipse.nebula.widgets.nattable.edit.editor.ICellEditor;
import org.eclipse.nebula.widgets.nattable.edit.editor.TextCellEditor;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayerListener;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.AggregateConfigLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.layer.cell.IConfigLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.layer.config.DefaultRowHeaderLayerConfiguration;
import org.eclipse.nebula.widgets.nattable.layer.event.CellVisualChangeEvent;
import org.eclipse.nebula.widgets.nattable.layer.event.CellVisualUpdateEvent;
import org.eclipse.nebula.widgets.nattable.layer.event.ILayerEvent;
import org.eclipse.nebula.widgets.nattable.layer.stack.DefaultBodyLayerStack;
import org.eclipse.nebula.widgets.nattable.painter.cell.ICellPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.decorator.PaddingDecorator;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.HorizontalAlignmentEnum;
import org.eclipse.nebula.widgets.nattable.style.IStyle;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.ui.action.IMouseAction;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.CellEditorMouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

public class GridEditorPage extends AbstractTestEditorFormPage implements SegmentSelectable, IActionBarsPopulator {

	private static final String ID = "grid";

	private static final String ROW_HEADER_EDIT_LABEL = "ROW_HEADER_EDIT";

	private ComboViewer cvSegment;

	private NatTable grid;

	private SelectionLayer selectionLayer;

	private Color clYellow;

	private Color clRed;

	private Clipboard clipboard;

	private DataLayer dataLayer;

	public GridEditorPage(TestDataEditor editor) {
		super(editor, ID, "Grid Editor");
		clYellow = new Color(editor.getSite().getShell().getDisplay(), 255, 255, 219);
		clRed = new Color(editor.getSite().getShell().getDisplay(), 255, 0, 0);
		clipboard = new Clipboard(editor.getSite().getShell().getDisplay());
	}

	@Override
	public void dispose() {
		clYellow.dispose();
		clRed.dispose();
		clipboard.dispose();
		super.dispose();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		form.setText("Test Data Grid");
		form.getBody().setLayout(new GridLayout(1, false));
		FormToolkit toolkit = managedForm.getToolkit();

		Section section = toolkit.createSection(form.getBody(), Section.DESCRIPTION | Section.EXPANDED | Section.TITLE_BAR);
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		section.setText("Edit Segment Data");
		section.setDescription("Select the test data segment to edit.");

		Composite c = toolkit.createComposite(section, SWT.WRAP);
		GridLayout layout = new GridLayout(2, false);
		c.setLayout(layout);
		section.setClient(c);

		toolkit.createLabel(c, "Segment:", SWT.LEAD);

		CCombo cbo = new CCombo(c, SWT.LEAD | SWT.BORDER);
		cbo.setEditable(false);
		toolkit.adapt(cbo);
		cvSegment = new ComboViewer(cbo);
		cvSegment.setContentProvider(new ConfigurationSegmentsContentProvider());
		cvSegment.setLabelProvider(new ConfigurationSegmentsLabelProvider());
		cvSegment.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				refreshGrid();
			}
		});
		
		ITestData testData = getTestDataModel();

		// build body stack
		dataLayer = new DataLayer(new GridDataProvider(testData));

		DefaultBodyLayerStack bodyLayer = new DefaultBodyLayerStack(dataLayer);
		bodyLayer.addConfiguration(new AbstractUiBindingConfiguration() {
			@Override
			public void configureUiBindings(UiBindingRegistry uiBindingRegistry) {
				uiBindingRegistry.registerDoubleClickBinding(new OpenReferenceMouseEventMatcher(), new IMouseAction() {
					@Override
					public void run(NatTable natTable, MouseEvent event) {
						handleOpenReference(event);
					}
				});

			}
		});
		selectionLayer = bodyLayer.getSelectionLayer();

		// build row stack
		DataLayer rowHeaderData = new DataLayer(new GridRowHeaderProvider(testData));
		RowHeaderLayer rowHeader = new RowHeaderLayer(rowHeaderData, bodyLayer, bodyLayer.getSelectionLayer(), false);
		rowHeaderData.setConfigLabelAccumulator(new IConfigLabelAccumulator() {
			@Override
			public void accumulateConfigLabels(LabelStack configLabels, int columnPosition, int rowPosition) {
				configLabels.addLabelOnTop(ROW_HEADER_EDIT_LABEL);
			}
		});
		rowHeader.addConfiguration(new NoResizeRowHeaderLayerConfiguration());
		rowHeaderData.addLayerListener(new ILayerListener() {
			@Override
			public void handleLayerEvent(ILayerEvent event) {
				if ((event instanceof CellVisualUpdateEvent) || (event instanceof CellVisualChangeEvent)) {
					// recalculate widths
					refreshGrid();
				}
			}
		});
		
		// build column stack
		DataLayer columnHeaderData = new DataLayer(new GridColumnHeaderProvider());
		ColumnHeaderLayer columnHeader = new ColumnHeaderLayer(columnHeaderData, bodyLayer, bodyLayer.getSelectionLayer());
		ErrorColumnMarker colMarker = new ErrorColumnMarker(columnHeaderData.getDataProvider());
		AggregateConfigLabelAccumulator aggrCla = new AggregateConfigLabelAccumulator();
		IConfigLabelAccumulator cla = columnHeader.getConfigLabelAccumulator();
		if (cla != null) {
			aggrCla.add(cla);
		}
		aggrCla.add(colMarker);
		columnHeaderData.setConfigLabelAccumulator(aggrCla);

		CornerLayer corner = new CornerLayer(new DataLayer(new DefaultCornerDataProvider(columnHeaderData.getDataProvider(),
				rowHeaderData.getDataProvider())), rowHeader, columnHeader);

		GridLayer gridLayer = new GridLayer(bodyLayer, columnHeader, rowHeader, corner);
		grid = new NatTable(c, gridLayer, false);
		grid.addConfiguration(new DefaultNatTableStyleConfiguration());
		grid.addConfiguration(new ColumnHeaderMenuConfiguration());
		grid.addConfiguration(new RowHeaderMenuConfiguration());
		grid.configure();

		toolkit.adapt(grid);
		toolkit.paintBordersFor(grid);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		grid.setLayoutData(gd);

		Font font = cvSegment.getControl().getFont();
		IStyle style = grid.getConfigRegistry().getConfigAttribute(CellConfigAttributes.CELL_STYLE, DisplayMode.NORMAL,
				"ROW_HEADER");
		style.setAttributeValue(CellStyleAttributes.FONT, font);
		style.setAttributeValue(CellStyleAttributes.HORIZONTAL_ALIGNMENT, HorizontalAlignmentEnum.LEFT);
		style = grid.getConfigRegistry().getConfigAttribute(CellConfigAttributes.CELL_STYLE, DisplayMode.NORMAL, "COLUMN_HEADER");
		style.setAttributeValue(CellStyleAttributes.FONT, font);

		ICellPainter painter = grid.getConfigRegistry().getConfigAttribute(CellConfigAttributes.CELL_PAINTER, DisplayMode.NORMAL);
		PaddingDecorator padder = new PaddingDecorator(painter);
		grid.getConfigRegistry().registerConfigAttribute(CellConfigAttributes.CELL_PAINTER, padder);
		grid.getConfigRegistry().registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new DefaultDisplayConverter(),
				DisplayMode.EDIT, ROW_HEADER_EDIT_LABEL);
		grid.getConfigRegistry().registerConfigAttribute(CellConfigAttributes.DISPLAY_CONVERTER, new GridLabelProvider());

		CellScriptMarker marker = new CellScriptMarker(dataLayer.getDataProvider());
		aggrCla = new AggregateConfigLabelAccumulator();
		cla = bodyLayer.getConfigLabelAccumulator();
		if (cla != null) {
			aggrCla.add(cla);
		}
		aggrCla.add(marker);
		bodyLayer.setConfigLabelAccumulator(aggrCla);

		// and register special background for script
		style = grid.getConfigRegistry().getConfigAttribute(CellConfigAttributes.CELL_STYLE, DisplayMode.SELECT);
		style.setAttributeValue(CellStyleAttributes.FONT,
				grid.getConfigRegistry().getConfigAttribute(CellConfigAttributes.CELL_STYLE, DisplayMode.NORMAL)
						.getAttributeValue(CellStyleAttributes.FONT));
		style = new Style();
		style.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, clYellow);
		grid.getConfigRegistry().registerConfigAttribute(CellConfigAttributes.CELL_STYLE, style, DisplayMode.NORMAL,
				"SCRIPT_VALUE");

		style = new Style();
		style.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, clRed);
		grid.getConfigRegistry().registerConfigAttribute(CellConfigAttributes.CELL_STYLE, style, DisplayMode.NORMAL,
				"ERROR_COLUMN");

		grid.getConfigRegistry().registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE,
				DisplayMode.EDIT, "EDITABLE_VALUE");
		grid.getConfigRegistry().registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, IEditableRule.ALWAYS_EDITABLE,
				DisplayMode.EDIT, ROW_HEADER_EDIT_LABEL);

		// some hack due to (my) bad API
		final GridFieldValueCellEditor[] editorArray = new GridFieldValueCellEditor[1];
		final GridFieldValueCellEditor editor = new GridFieldValueCellEditor(this, new RefreshFieldHandler() {
			@Override
			public void update(ITestDataFieldValue field) {
				int row = editorArray[0].getRowIndex();
				int col = editorArray[0].getColumnIndex();
				// just clear cache for this field
				dataLayer.setDataValue(col, row, null);
				grid.refresh();
			}
		});
		editorArray[0] = editor;

		grid.getConfigRegistry().registerConfigAttribute(EditConfigAttributes.CELL_EDITOR, editor,
				DisplayMode.EDIT, "EDITABLE_VALUE");
		grid.getConfigRegistry().registerConfigAttribute(EditConfigAttributes.CELL_EDITOR, new TextCellEditor(),
				DisplayMode.EDIT, ROW_HEADER_EDIT_LABEL);
		grid.getConfigRegistry().registerConfigAttribute(EditConfigAttributes.DATA_VALIDATOR, new ConfigNameDataValidator(),
				DisplayMode.EDIT, ROW_HEADER_EDIT_LABEL);
		grid.getConfigRegistry().registerConfigAttribute(EditConfigAttributes.VALIDATION_ERROR_HANDLER,
				new RenderErrorHandling(), DisplayMode.EDIT, ROW_HEADER_EDIT_LABEL);

		refreshContents();
	}

	@Override
	protected void refreshContents() {
		ITestData testData = getEditor().getTestDataModel();
		if (cvSegment != null) {
			cvSegment.setInput(testData);
			refreshGrid();
		}
	}

	private void refreshGrid() {
		((GridDataProvider) dataLayer.getDataProvider()).refresh();

		ITestData testData = getEditor().getTestDataModel();
		grid.refresh();

		GridLayer gl = (GridLayer) grid.getLayer();

		// refresh column header
		ColumnHeaderLayer chl = (ColumnHeaderLayer) gl.getColumnHeaderLayer();
		((GridColumnHeaderProvider) ((DataLayer) chl.getBaseLayer()).getDataProvider()).refresh();

		// auto-size row header
		RowHeaderLayer rhl = (RowHeaderLayer) gl.getRowHeaderLayer();

		int width = 0;
		GC gc = new GC(grid);
		for (int i = 0; i < testData.getConfigurations().length; i++) {
			ILayerCell cell = grid.getCellByPosition(0, i + 1);
			if (cell != null) {
				ICellPainter painter = grid.getCellPainter(0, i + 1, cell, grid.getConfigRegistry());
				width = Math.max(width, painter.getPreferredWidth(cell, gc, grid.getConfigRegistry()));
			}
		}
		gc.dispose();
		DataLayer dl = (DataLayer) rhl.getBaseLayer();
		dl.setDefaultColumnWidth(Math.max(300, width + 10));
	}

	@Override
	public boolean selectSegment(String segmentName) {
		for (ITestDataSegmentMetadata segment : getEditor().getTestDataModel().getMetaData().getSegments()) {
			if (segment.getName().equals(segmentName)) {
				cvSegment.setSelection(new StructuredSelection(segment));
				refreshGrid();
				return true;
			}
		}

		return false;
	}

	@Override
	public void contributeToActionBars(IActionBars actionBars) {
		// add C&P actions
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), new CopyAction());
		actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), new PasteAction());
	}

	@Override
	public ITestData getTestDataModel() {
		return getEditor().getTestDataModel();
	}

	private ITestDataFieldMetadata getFieldMetadata(ITestDataSegmentMetadata segment, String fieldName) {
		for (ITestDataFieldMetadata field : segment.getFields()) {
			if (fieldName.equals(field.getName())) {
				return field;
			}
		}
		return null;
	}

	private CellInfo getCellInfo(ITestDataConfiguration config, ITestDataSegmentMetadata segment, String fieldName) {
		String segmentName = segment.getName();
		ITestDataFieldMetadata meta = getFieldMetadata(segment, fieldName);

		if (meta != null && (meta.getType() == TestDataFieldType.OBJECT || meta.getType() == TestDataFieldType.OBJECT_LIST)) {
			String refSegName = segmentName + "." + fieldName;
			if (meta.getType() == TestDataFieldType.OBJECT_LIST) {
				refSegName += "-1";
			}
			return new CellInfo(true, "(" + refSegName + ")", null, new SegmentReference(refSegName));
		}

		ITestDataConfigurationSegment configSegment = config.getSegment(segmentName);
		if (configSegment != null) {
			ITestDataFieldValue field = configSegment.getFieldValue(fieldName, false);

			if (field == null) {
				if (meta != null && meta.getType() == TestDataFieldType.STRING_LIST) {
					return new CellInfo("[]", configSegment, fieldName);
				}

				return new CellInfo("", configSegment, fieldName);
			}

			IFieldValue fv = field.getFieldValue();
			if (fv.getValueType() == IFieldValue.TYPE_STRING) {
				String value = ((IStringValue) fv).getValue();
				if (value == null) {
					value = "";
				}
				return new CellInfo(false, value, field, null);
			}
			if (fv.getValueType() == IFieldValue.TYPE_STRING_LIST) {
				List<String> values = new ArrayList<String>();
				for (IStringValue value : ((IStringListValue) fv).getValues()) {
					values.add(value.getValue());
				}
				return new CellInfo(false, values.toString(), field, null);
			}
		}

		return null;

	}

	private void handleOpenReference(MouseEvent event) {
		int col = grid.getColumnPositionByX(event.x);
		int row = grid.getRowPositionByY(event.y);
		ILayerCell cell = grid.getCellByPosition(col, row);
		if (cell != null) {
			CellInfo info = (CellInfo) cell.getDataValue();
			if (info != null && info.isReference && info.reference != null) {
				selectSegment(info.reference.getSegmentName());
			}
		}
	}

	private ITestDataSegmentMetadata getSelectedSegment() {
		return (ITestDataSegmentMetadata) ((IStructuredSelection) cvSegment.getSelection()).getFirstElement();
	}

	private static class ConfigurationSegmentsContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			ITestData data = (ITestData) inputElement;
			return data.getMetaData().getSegments();
		}

	}

	private static class ConfigurationSegmentsLabelProvider extends LabelProvider {

		@Override
		public String getText(Object element) {
			ITestDataSegmentMetadata segment = (ITestDataSegmentMetadata) element;
			return segment.getName();
		}

	}

	private class GridDataProvider implements IDataProvider {

		private ITestData testData;

		private Map<Point, Object> cellInfoCache = new HashMap<Point, Object>();

		private ITestDataSegmentMetadata segment;

		private Integer rowCount;

		private Integer colCount;

		public GridDataProvider(ITestData testData) {
			this.testData = testData;
		}

		@Override
		public Object getDataValue(int columnIndex, int rowIndex) {
			if (segment == null) {
				return null;
			}

			Point pt = new Point(columnIndex, rowIndex);
			if (cellInfoCache.containsKey(pt)) {
				return cellInfoCache.get(pt);
			}

			// to make things safe, get column header to determine field name
			GridLayer gl = (GridLayer) grid.getLayer();
			ColumnHeaderLayer chl = (ColumnHeaderLayer) gl.getColumnHeaderLayer();
			String fieldName = (String) ((DataLayer) chl.getBaseLayer()).getDataProvider().getDataValue(columnIndex, 0);

			ITestDataConfiguration config = testData.getConfigurations()[rowIndex];

			CellInfo ci = getCellInfo(config, segment, fieldName);
			cellInfoCache.put(pt, ci);
			return ci;
		}

		@Override
		public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
			// refresh cache on this position
			cellInfoCache.remove(new Point(columnIndex, rowIndex));
		}

		@Override
		public int getColumnCount() {
			if (colCount == null) {
				colCount = segment == null ? 0 : segment.getFields().length;
			}
			return colCount.intValue();
		}

		@Override
		public int getRowCount() {
			if (rowCount == null) {
				rowCount = testData.getConfigurations().length;
			}
			return rowCount.intValue();
		}

		public synchronized void refresh() {
			cellInfoCache.clear();
			rowCount = null;
			colCount = null;
			segment = getSelectedSegment();
		}
	}

	private class GridColumnHeaderProvider implements IDataProvider {

		private String segmentName;

		private List<String> cachedColumns;

		@Override
		public Object getDataValue(int columnIndex, int rowIndex) {
			return getConfigurationFieldsSuperset().get(columnIndex);
		}

		@Override
		public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getColumnCount() {
			return getConfigurationFieldsSuperset().size();
		}

		@Override
		public int getRowCount() {
			return 1;
		}

		public void refresh() {
			cachedColumns = null;
		}

		private List<String> getConfigurationFieldsSuperset() {
			ITestDataSegmentMetadata segment = getSelectedSegment();
			if (segment == null) {
				return Collections.emptyList();
			}
			
			String segmentName = segment.getName();
			if (cachedColumns != null && segmentName.equals(this.segmentName)) {
				return cachedColumns;
			}

			// first of all, collect fields from metadata
			List<String> result = new ArrayList<String>();
			for (ITestDataFieldMetadata field : segment.getFields()) {
				result.add(field.getName());
			}
			
			// now, add all "red" fields (not present in metadata)
			List<String> errorFields = new ArrayList<String>();

			ITestDataConfiguration[] configs = getTestDataModel().getConfigurations();
			
			for (ITestDataConfiguration config : configs) {
				ITestDataConfigurationSegment configSegment = config.getSegment(segmentName);
				if (configSegment == null) {
					continue;
				}

				for (ITestDataFieldValue field : configSegment.getDefinedFieldValues()) {
					String fn = field.getFieldName();
					if (!result.contains(fn) && !errorFields.contains(fn)) {
						errorFields.add(fn);
					}
				}
			}
			
			result.addAll(0, errorFields);
			this.segmentName = segmentName;
			return cachedColumns = result;
		}

	}

	private static class GridRowHeaderProvider implements IDataProvider {
		private ITestData testData;

		public GridRowHeaderProvider(ITestData testData) {
			this.testData = testData;
		}

		@Override
		public Object getDataValue(int columnIndex, int rowIndex) {
			return testData.getConfigurations()[rowIndex].getName();
		}

		@Override
		public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
			if (newValue != null && !"".equals(newValue.toString().trim())) {
				testData.getConfigurations()[rowIndex].setName(newValue.toString());
			}
		}

		@Override
		public int getColumnCount() {
			return 1;
		}

		@Override
		public int getRowCount() {
			return testData.getConfigurations().length;
		}
	}

	private static class GridLabelProvider implements IDisplayConverter {

		@Override
		public Object canonicalToDisplayValue(Object canonicalValue) {
			if (canonicalValue instanceof CellInfo) {
				return ((CellInfo) canonicalValue).displayText;
			}
			return canonicalValue;
		}

		@Override
		public Object displayToCanonicalValue(Object displayValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object canonicalToDisplayValue(ILayerCell cell, IConfigRegistry configRegistry, Object canonicalValue) {
			return canonicalToDisplayValue(canonicalValue);
		}

		@Override
		public Object displayToCanonicalValue(ILayerCell cell, IConfigRegistry configRegistry, Object displayValue) {
			throw new UnsupportedOperationException();
		}

	}

	static class CellInfo {

		private boolean isReference;

		private String displayText;

		private ITestDataFieldValue fieldValue;

		private SegmentReference reference;

		private ITestDataConfigurationSegment configSegment;

		private String fieldName;

		public CellInfo(boolean isReference, String displayText, ITestDataFieldValue fieldValue, SegmentReference reference) {
			this.isReference = isReference;
			this.displayText = displayText;
			this.fieldValue = fieldValue;
			this.reference = reference;
		}

		public CellInfo(String displayText, ITestDataConfigurationSegment configSegment, String fieldName) {
			this.configSegment = configSegment;
			this.fieldName = fieldName;
		}

		public ITestDataFieldValue getFieldValueWithoutCreate() {
			if (fieldValue == null && configSegment != null && fieldName != null) {
				fieldValue = configSegment.getFieldValue(fieldName, false);
			}
			return fieldValue;
		}

		public ITestDataFieldValue getFieldValue() {
			if (fieldValue == null && configSegment != null && fieldName != null) {
				fieldValue = configSegment.getFieldValue(fieldName, true);
			}
			return fieldValue;
		}
	}

	private static class CellScriptMarker implements IConfigLabelAccumulator {

		private IDataProvider dataProvider;

		public CellScriptMarker(IDataProvider dataProvider) {
			this.dataProvider = dataProvider;
		}

		@Override
		public void accumulateConfigLabels(LabelStack configLabels, int columnPosition, int rowPosition) {
			Object o = dataProvider.getDataValue(columnPosition, rowPosition);
			if (o instanceof CellInfo) {
				CellInfo info = (CellInfo) o;
				if (info.isReference) {
					configLabels.addLabelOnTop("REFERENCE_VALUE");
				}
				if (!info.isReference) {
					configLabels.addLabelOnTop("EDITABLE_VALUE");
				}
				if (!info.isReference && info.fieldValue != null && info.fieldValue.isScript()) {
					configLabels.addLabelOnTop("SCRIPT_VALUE");
				}
			}
		}

	}

	private class ErrorColumnMarker implements IConfigLabelAccumulator {

		private IDataProvider dataProvider;

		public ErrorColumnMarker(IDataProvider dataProvider) {
			this.dataProvider = dataProvider;
		}

		@Override
		public void accumulateConfigLabels(LabelStack configLabels, int columnPosition, int rowPosition) {
			String segName = (String) dataProvider.getDataValue(columnPosition, rowPosition);
			ITestDataSegmentMetadata segment = getSelectedSegment();

			for (ITestDataFieldMetadata field : segment.getFields()) {
				if (segName.equals(field.getName())) {
					return;
				}
			}

			// not found in segment; add error label
			configLabels.addLabelOnTop("ERROR_COLUMN");
		}

	}

	private class ColumnHeaderMenuConfiguration extends AbstractUiBindingConfiguration {

		private Menu contextMenu;

		public ColumnHeaderMenuConfiguration() {
			contextMenu = createContextMenu(grid);
		}

		private Menu createContextMenu(NatTable natTable) {
			Menu menu = new Menu(natTable);

			MenuItem itemDelete = new MenuItem(menu, SWT.NONE);
			itemDelete.setText("Delete from all configurations");

			ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
			itemDelete.setImage(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE).createImage());
			itemDelete.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					MenuItem item = (MenuItem) e.widget;
					Integer selectedColumn = (Integer) item.getParent().getData("selectedColumn");
					if (selectedColumn != null) {
						deleteFieldFromConfigurations(selectedColumn.intValue());
					}
				}
			});

			return new PopupMenuBuilder(natTable, menu).build();
		}
		
		private void deleteFieldFromConfigurations(int selectedColumnIndex) {
			// get name of that field
			GridLayer gl = (GridLayer) grid.getLayer();
			ColumnHeaderLayer chl = (ColumnHeaderLayer) gl.getColumnHeaderLayer();
			String fieldName = (String) ((DataLayer) chl.getBaseLayer()).getDataProvider().getDataValue(selectedColumnIndex, 0);
			if (fieldName != null) {
				ITestDataSegmentMetadata segment = getSelectedSegment();
				if (segment == null) {
					return;
				}
				String segmentName = segment.getName();

				for (ITestDataConfiguration config : getTestDataModel().getConfigurations()) {
					ITestDataConfigurationSegment configSegment = config.getSegment(segmentName);
					if (configSegment instanceof TestDataConfigurationSegment) {
						ITestDataFieldValue fv = configSegment.getFieldValue(fieldName, false);
						if (fv != null) {
							((TestDataConfigurationSegment) configSegment).removeFieldValue(fv);
						}
					}
				}
			}

			refreshGrid();
		}

		@Override
		public void configureUiBindings(UiBindingRegistry uiBindingRegistry) {
			MouseEventMatcher matcher = new MouseEventMatcher(SWT.NONE, GridRegion.COLUMN_HEADER, 3) {

				@Override
				public boolean matches(NatTable natTable, MouseEvent event, LabelStack regionLabels) {
					if (!super.matches(natTable, event, regionLabels)) {
						return false;
					}
					// cell at pos must contain the ERROR_COLUMN label
					int col = natTable.getColumnPositionByX(event.x);
					LabelStack cellLabels = natTable.getConfigLabelsByPosition(col, 0);

					return cellLabels != null && cellLabels.hasLabel("ERROR_COLUMN");
				}

			};

			uiBindingRegistry.registerMouseDownBinding(matcher, new ColumnHeaderPopupMenuAction(contextMenu));
		}
	}

	private static class ColumnHeaderPopupMenuAction implements IMouseAction {

		private final Menu menu;

		public ColumnHeaderPopupMenuAction(Menu menu) {
			this.menu = menu;
		}

		@Override
		public void run(NatTable natTable, MouseEvent event) {
			int col = natTable.getColumnPositionByX(event.x);
			// mark which column it is - subtract 1 because of row header column
			menu.setData("selectedColumn", Integer.valueOf(col - 1));
			menu.setVisible(true);
		}
	}

	private class RowHeaderMenuConfiguration extends AbstractUiBindingConfiguration {

		private Menu contextMenu;

		public RowHeaderMenuConfiguration() {
			contextMenu = createContextMenu(grid);
		}

		private Menu createContextMenu(NatTable natTable) {
			Menu menu = new Menu(natTable);

			MenuItem itemDelete = new MenuItem(menu, SWT.NONE);
			itemDelete.setText("Delete configuration(s)");

			ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
			itemDelete.setImage(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE).createImage());
			itemDelete.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					deleteSelectedConfigurations();
				}
			});

			return new PopupMenuBuilder(natTable, menu).build();
		}

		private void deleteSelectedConfigurations() {
			Set<Range> selectedRowRanges = selectionLayer.getSelectedRowPositions();

			ITestData testData = getTestDataModel();

			// to avoid any internal implementation errors, delete from the end, which makes
			// algorithm more complex than required
			List<Integer> selectedRows = new ArrayList<Integer>();

			for (Range range : selectedRowRanges) {
				for (int r = range.start; r < range.end; r++) {
					selectedRows.add(Integer.valueOf(r));
				}
			}

			Collections.sort(selectedRows);
			Collections.reverse(selectedRows);

			for (Integer row : selectedRows) {
				testData.removeConfiguration(testData.getConfigurations()[row.intValue()]);
			}

			refreshGrid();
		}

		@Override
		public void configureUiBindings(UiBindingRegistry uiBindingRegistry) {
			MouseEventMatcher matcher = new MouseEventMatcher(SWT.NONE, GridRegion.ROW_HEADER, 3) {
				@Override
				public boolean matches(NatTable natTable, MouseEvent event, LabelStack regionLabels) {
					if (!super.matches(natTable, event, regionLabels)) {
						return false;
					}
					Set<Range> selectedRowRanges = selectionLayer.getSelectedRowPositions();
					return !selectedRowRanges.isEmpty();
				}
			};
			uiBindingRegistry.registerMouseDownBinding(matcher, new RowHeaderPopupMenuAction(contextMenu));
		}
	}

	private static class RowHeaderPopupMenuAction implements IMouseAction {

		private final Menu menu;

		public RowHeaderPopupMenuAction(Menu menu) {
			this.menu = menu;
		}

		@Override
		public void run(NatTable natTable, MouseEvent event) {
			int row = natTable.getRowPositionByY(event.y);
			// mark which rows it is - subtract 1 because of column header row
			menu.setData("selectedRow", Integer.valueOf(row - 1));
			menu.setVisible(true);
		}
	}

	private static class NoResizeRowHeaderLayerConfiguration extends DefaultRowHeaderLayerConfiguration {
		@Override
		protected void addRowHeaderUIBindings() {
		}

		@Override
		public void configureUiBindings(UiBindingRegistry uiBindingRegistry) {
			super.configureUiBindings(uiBindingRegistry);
			uiBindingRegistry.registerSingleClickBinding(new CellEditorMouseEventMatcher(GridRegion.ROW_HEADER),
					new MouseEditAction());
		}

	}

	private static class OpenReferenceMouseEventMatcher extends MouseEventMatcher {

		public OpenReferenceMouseEventMatcher() {
			super(0, "BODY", LEFT_BUTTON);
		}

		@Override
		public boolean matches(NatTable natTable, MouseEvent event, LabelStack regionLabels) {
			if (!super.matches(natTable, event, regionLabels)) {
				return false;
			}

			// only supported on REFERENCE_VALUE cells
			int col = natTable.getColumnPositionByX(event.x);
			int row = natTable.getRowPositionByY(event.y);
			ILayerCell cell = natTable.getCellByPosition(col, row);
			if (cell != null) {
				CellInfo info = (CellInfo) cell.getDataValue();
				return info.isReference;
			}

			return false;
		}
	}

	private class CopyAction extends Action {

		@Override
		public void run() {
			// if a cell editor is active, pass to it
			if (grid != null && grid.getActiveCellEditor() != null) {
				ICellEditor editor = grid.getActiveCellEditor();
				if (editor instanceof GridFieldValueCellEditor) {
					((GridFieldValueCellEditor) editor).copy();
				}
				return;
			}

			if (selectionLayer != null) {
				ClipboardUtil.copyToClipboard(clipboard, selectionLayer.getSelectedCells(),
						new UnsafeConverter<ILayerCell, ITestDataFieldValue>(ILayerCell.class, ITestDataFieldValue.class) {
							@Override
							public ITestDataFieldValue convert(ILayerCell cell) throws ConversionException {
								Object o = cell.getDataValue();
								if (o instanceof CellInfo) {
									return ((CellInfo) o).fieldValue;
								}
								return null;
							}
				});
			}
		}

		@Override
		public void runWithEvent(Event event) {
			run();
		}

		@Override
		public boolean isEnabled() {
			return selectionLayer != null && !selectionLayer.getSelectedCells().isEmpty();
		}

	}

	private class PasteAction extends Action {

		@Override
		public void run() {
			// if a cell editor is active, pass to it
			if (grid != null && grid.getActiveCellEditor() != null) {
				ICellEditor editor = grid.getActiveCellEditor();
				if (editor instanceof GridFieldValueCellEditor) {
					((GridFieldValueCellEditor) editor).paste();
				}
				return;
			}

			if (selectionLayer != null) {
				try {
					ClipboardUtil.pasteFromClipboard(clipboard, selectionLayer, new PasteStringValueAcceptor() {
						@Override
						public void accept(int columnIndex, int rowIndex, String value, boolean script) {
							ILayerCell cell = selectionLayer.getCellByPosition(columnIndex, rowIndex);
							// cell could e.g. be null if pasted data is more than selection (which is OK)
							if (cell != null) {
								CellInfo info = (CellInfo) cell.getDataValue();
								if (info != null && info.getFieldValue() != null) {
									IFieldValue fv = info.fieldValue.getFieldValue();
									if (fv != null && fv.getValueType() == IFieldValue.TYPE_STRING) {
										((IStringValue) fv).setValue(value);
										info.fieldValue.setScript(script);
									}
									// force update in data layer
									dataLayer.getDataProvider().setDataValue(columnIndex, rowIndex, value);
								}
								else {
									// should never be null, only for referencing cells which cannot be set
								}
							}
						}
					});
					grid.refresh();
				}
				catch (ClipboardRegionDoesNotMatchException e) {
					MessageDialog.openError(getEditorSite().getShell(), "Error", e.getMessage());
				}
			}
		}

		@Override
		public void runWithEvent(Event event) {
			run();
		}

		@Override
		public boolean isEnabled() {
			return ClipboardUtil.canPasteFromClipboard(clipboard);
		}

	}

	private class ConfigNameDataValidator extends ContextualDataValidator {

		@Override
		public boolean validate(ILayerCell cell, IConfigRegistry configRegistry, Object newValue) {
			// get all config names; remove old value
			List<String> configNames = new ArrayList<String>();
			for (ITestDataConfiguration config : getEditor().getTestDataModel().getConfigurations()) {
				configNames.add(config.getName());
			}
			configNames.remove(cell.getDataValue());

			// delegate to VisualDataEditorPage helper
			VisualDataEditorPage.ConfigNameValidator delegate = new VisualDataEditorPage.ConfigNameValidator(configNames);

			String errorMessage = delegate.isValid(newValue == null ? "" : newValue.toString());
			if (errorMessage != null) {
				throw new ValidationFailedException(errorMessage);
			}

			return true;
		}

	}

}
