import React from 'react';
import { ChipViewItemModel } from '../types';

interface ChipSelectorProps {
  items: ChipViewItemModel[];
  onSelectionChange: (selectedItems: ChipViewItemModel[]) => void;
  title: string;
  multiSelect?: boolean;
}

const ChipSelector: React.FC<ChipSelectorProps> = ({ 
  items, 
  onSelectionChange, 
  title, 
  multiSelect = true 
}) => {
  const handleChipClick = (clickedItem: ChipViewItemModel) => {
    let updatedItems: ChipViewItemModel[];
    
    if (multiSelect) {
      updatedItems = items.map(item => 
        item.name === clickedItem.name 
          ? { ...item, selected: !item.selected }
          : item
      );
    } else {
      updatedItems = items.map(item => ({
        ...item,
        selected: item.name === clickedItem.name ? !item.selected : false
      }));
    }
    
    onSelectionChange(updatedItems.filter(item => item.selected));
  };

  return (
    <div className="card">
      <h3 style={{ marginBottom: '16px', color: '#333' }}>{title}</h3>
      <div>
        {items.map((item) => (
          <span
            key={item.name}
            className={`chip ${item.selected ? 'selected' : ''}`}
            onClick={() => handleChipClick(item)}
          >
            {item.cultureValue}
          </span>
        ))}
      </div>
    </div>
  );
};

export default ChipSelector;
