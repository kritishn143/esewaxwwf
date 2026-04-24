import { NextResponse } from 'next/server';
import path from 'path';

const dataFile = path.join(process.cwd(), 'kyc_data.json');

export async function PUT(request, { params }) {
  try {
    const { id } = await params;
    const updates = await request.json();
    
    const currentData = JSON.parse(require('fs').readFileSync(dataFile, 'utf8'));
    const index = currentData.findIndex(item => item.id === id);
    
    if (index === -1) {
      return NextResponse.json({ success: false, error: 'Not found' }, { status: 404 });
    }

    if (updates.status === 'DECLINED' && updates.feedback) {
      if (!currentData[index].history) currentData[index].history = [];
      currentData[index].history.push({
        status: 'DECLINED',
        feedback: updates.feedback,
        timestamp: new Date().toISOString()
      });
    }

    // Remove history from updates so it doesn't overwrite the push we just did
    delete updates.history;

    currentData[index] = { ...currentData[index], ...updates };
    require('fs').writeFileSync(dataFile, JSON.stringify(currentData, null, 2));

    return NextResponse.json({ success: true, data: currentData[index] });
  } catch (error) {
    console.error('API Error:', error);
    return NextResponse.json({ success: false, error: error.message }, { status: 500 });
  }
}

export async function GET(request, { params }) {
  try {
    const { id } = await params;
    const currentData = JSON.parse(require('fs').readFileSync(dataFile, 'utf8'));
    const record = currentData.find(item => item.id === id);
    
    if (!record) {
      return NextResponse.json({ success: false, error: 'Not found' }, { status: 404 });
    }

    return NextResponse.json(record);
  } catch (error) {
    console.error('API Error:', error);
    return NextResponse.json({ success: false, error: error.message }, { status: 500 });
  }
}

export async function DELETE(request, { params }) {
  try {
    const { id } = await params;
    const currentData = JSON.parse(require('fs').readFileSync(dataFile, 'utf8'));
    const index = currentData.findIndex(item => item.id === id);
    
    if (index === -1) {
      return NextResponse.json({ success: false, error: 'Not found' }, { status: 404 });
    }

    const record = currentData[index];
    
    // Delete images
    const fs = require('fs');
    ['frontImage', 'backImage', 'selfieImage'].forEach(field => {
      if (record[field]) {
        const filePath = path.join(process.cwd(), 'public', record[field]);
        if (fs.existsSync(filePath)) {
          fs.unlinkSync(filePath);
        }
      }
    });

    // Remove from array and save
    currentData.splice(index, 1);
    fs.writeFileSync(dataFile, JSON.stringify(currentData, null, 2));

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error('API Error:', error);
    return NextResponse.json({ success: false, error: error.message }, { status: 500 });
  }
}
