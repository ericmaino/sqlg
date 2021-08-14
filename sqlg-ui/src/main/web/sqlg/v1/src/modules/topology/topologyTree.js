import m from "mithril";
import SlickLazyTree from "../../components/tree/m.slick.lazytree";
import TopologyManager from "./TopologyManager";

function TopologyTree(ignore) {

    let tree;
    let contextMenuItem;
    let defaultOptions = {
        id: "sqlgSchemaTreeId",
        showSpinner: true,
        refreshData: false,
        selectedItems: [],
        data: {
            columns: [{id: "title", name: "Title", field: "title", width: 220}],
            dataNormal: {},
            options: {
                enableCellNavigation: true,
                explicitInitialization: true,
                editable: false,
                enableAddRow: false,
                // asyncCheckChildren: true,
                frozenColumn: 1
            }
        },
        filterFunction: function (grid, columnFilter, item) {
            let result = columnFilter === null || columnFilter === '';
            if (!result) {
                result = item.title.toLowerCase().includes(columnFilter.toLowerCase());
            }
            return result;
        },
        fetchChildren: function (item, callBack) {
            if (item.indent > 1) {
            } else {
                CmMithrilGlobal.toasts.push({
                    type: "warning",
                    message: item.title + " is not in the tree, try refreshing the tree.",
                    header: "Element browser",
                    autohide: false
                });
                m.redraw();
            }
        },
        tooltip: {
            event: 'click',
            has: function (item) {
                return false;
            }, html: function (item) {
                return "<div>halo</div>";
            }
        },
        slickLazyTreeCallBack: function (t) {
            tree = t;
        },
        contextMenuCallback: function (item, y, x) {
            contextMenuItem = item;
            m.redraw();
            let menu = $('#context-menu');
            menu.css({"display": "inline-block"})
            menu.offset({top: y, left: x});
        }
    };

    return {
        oninit: ({attrs: {actions}}) => {
            defaultOptions.selectedItemCallBack = (item) => {
                actions.retrieveSchemaDetails(item);
                m.redraw();
            }
        },
        onupdate: ({attrs: {actions}}) => {
            actions.setTreeRefresh(false);
        },
        view: ({attrs: {state}}) => {
            let {treeData} = state;
            defaultOptions.data.dataNormal = treeData.data;
            defaultOptions.refreshData = treeData.refreshData;
            defaultOptions.showSpinner = treeData.spin;
            defaultOptions.selectedItem = treeData.selectedTreeItem;
            defaultOptions.refresh = treeData;
            defaultOptions.refreshActive = treeData.refreshActive;
            return m(SlickLazyTree, defaultOptions)
        }
    }

}

export default TopologyTree;