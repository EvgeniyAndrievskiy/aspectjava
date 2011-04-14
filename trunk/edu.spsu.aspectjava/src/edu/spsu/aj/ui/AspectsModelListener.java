package edu.spsu.aj.ui;



interface AspectsModelListener{	
	void aspectsContainerAdded(AspectsModel.AspectsContainer container);

	void aspectsContainerRemoved(AspectsModel.AspectsContainer container, int index);
	
	void containerMovedDown(AspectsModel.AspectsContainer container);
	
	void containerMovedUp(AspectsModel.AspectsContainer container);

	void aspectsContainerAdded(int index, AspectsModel.AspectsContainer container);
}
